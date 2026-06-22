package com.maksymchernenko.railwayticketsystem.service.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsResponse;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class AdminAnalyticsTools {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_TOOL_LIMIT = 60;
    private static final Duration TRANSFER_WINDOW = Duration.ofHours(6);
    private static final List<Ticket.Status> SOLD_TICKET_STATUSES = List.of(Ticket.Status.ACTIVE, Ticket.Status.FINISHED);

    private final AdminAnalyticsService adminAnalyticsService;
    private final TripRepository tripRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StationRepository stationRepository;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocal<SelectedAnalyticsFilters> selectedAnalyticsFilters = new ThreadLocal<>();

    public <T> T withSelectedAnalyticsFilters(SelectedAnalyticsFilters filters, Supplier<T> action) {
        selectedAnalyticsFilters.set(filters);
        try {
            return action.get();
        } finally {
            selectedAnalyticsFilters.remove();
        }
    }

    @Tool("Returns the current backend server time in UTC and in the application timezone. Use it for questions about upcoming trips or current state.")
    public CurrentTimeInfo getCurrentSystemTime() {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        return new CurrentTimeInfo(now.toString(), zoneId.toString(), now.atZone(zoneId).toString());
    }

    @Tool("Returns admin analytics for the exact filters currently selected in the admin UI. Use this first for questions about current table values, selected period, profitability, popular segments, demand, occupancy, or sales trend.")
    public AdminAnalyticsResponse getSelectedAnalyticsOverview() {
        SelectedAnalyticsFilters filters = selectedAnalyticsFilters.get();
        if (filters == null) {
            throw new IllegalStateException("Selected admin analytics filters are not available for this request.");
        }

        return inTransaction(() -> adminAnalyticsService.getAnalytics(new AdminAnalyticsRequest(
                filters.dateFrom(),
                filters.dateTo(),
                filters.routeId(),
                filters.limit()
        )));
    }

    @Tool("Returns compact admin analytics for an explicitly requested period that differs from the current admin UI filters.")
    public AdminAnalyticsResponse getAnalyticsOverview(
            @P("ISO-8601 start instant, inclusive, for example 2026-06-01T00:00:00Z") String dateFrom,
            @P("ISO-8601 end instant, exclusive, for example 2026-07-01T00:00:00Z") String dateTo,
            @P("Optional route id. Use null to analyze all routes.") Long routeId,
            @P("Maximum rows per list. Use 5-20 for concise answers.") Integer limit
    ) {
        return inTransaction(() -> adminAnalyticsService.getAnalytics(new AdminAnalyticsRequest(
                        parseInstant(dateFrom, Instant.now().minus(30, ChronoUnit.DAYS)),
                        parseInstant(dateTo, Instant.now().plus(1, ChronoUnit.DAYS)),
                        routeId,
                        normalizeLimit(limit)
                )));
    }

    @Tool("Returns upcoming or scheduled trips with route, stops, capacity, sold seats, occupancy, and car count. Use it for operational questions about problematic or nearest trips.")
    public List<TripContext> getTripsInPeriod(
            @P("ISO-8601 start instant, inclusive. Use current time for nearest upcoming trips.") String dateFrom,
            @P("ISO-8601 end instant, exclusive.") String dateTo,
            @P("Optional route id. Use null to include all routes.") Long routeId,
            @P("Maximum number of trips to return.") Integer limit
    ) {
        return inTransaction(() -> {
            Instant from = parseInstant(dateFrom, Instant.now());
            Instant to = parseInstant(dateTo, from.plus(2, ChronoUnit.DAYS));
            List<Trip> trips = tripRepository.findTripsInPeriod(from, to, routeId, PageRequest.of(0, normalizeLimit(limit)));
            Map<Long, TripRepository.TripCapacityRow> capacityByTripId = getCapacityByTripId(trips);

            return trips.stream()
                    .map(trip -> toTripContext(trip, capacityByTripId.get(trip.getId())))
                    .toList();
        });
    }

    @Tool("Finds routes by route number, route name, or station name and returns their ordered station stops. Use before route-specific analysis if the user names stations or route text.")
    public List<RouteContext> findRoutes(
            @P("Route number, route name, station name, or route fragment from the administrator request.") String query,
            @P("Maximum number of routes to return.") Integer limit
    ) {
        return inTransaction(() -> {
            String normalizedQuery = query == null ? "" : query.trim();
            List<Route> routes = findRoutesForQuery(normalizedQuery);

            return routes.stream()
                    .sorted(Comparator.comparing(Route::getNumber))
                    .limit(normalizeLimit(limit))
                    .map(this::toRouteContext)
                    .toList();
        });
    }

    @Tool("Returns the route network with ordered station stops. Use it to reason about missing direct routes, station connectivity, and possible route extensions.")
    public List<RouteContext> getRouteNetwork(
            @P("Maximum number of routes to return. Use a moderate value to keep the answer concise.") Integer limit
    ) {
        return inTransaction(() -> routeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(normalizeLimit(limit))
                .map(this::toRouteContext)
                .toList());
    }

    public String getCatalogPromptText() {
        return inTransaction(() -> {
            String stations = stationRepository.findAll().stream()
                    .sorted(Comparator.comparing(Station::getName))
                    .map(station -> "%s [%s]".formatted(station.getName(), station.getCountry().getCode()))
                    .reduce((first, second) -> first + "; " + second)
                    .orElse("No stations configured.");

            String routes = routeRepository.findAll().stream()
                    .sorted(Comparator.comparing(Route::getNumber))
                    .map(route -> "routeId=%s, number=%s, name=%s, stops=%s".formatted(
                            route.getId(),
                            route.getNumber(),
                            route.getName(),
                            routeStopsText(route.getId())
                    ))
                    .reduce((first, second) -> first + "\n" + second)
                    .orElse("No routes configured.");

            return """
                    Available stations from the database:
                    %s

                    Available route network from the database:
                    %s
                    """.formatted(stations, routes);
        });
    }

    @Tool("Returns transfer opportunities and missed connections for a route within a period. Use it for schedule optimization and departure time recommendations.")
    public TransferContext getTransferContext(
            @P("Route id to analyze. Find it with findRoutes if the user gives route text.") Long routeId,
            @P("ISO-8601 start instant, inclusive.") String dateFrom,
            @P("ISO-8601 end instant, exclusive.") String dateTo,
            @P("Maximum number of source trips and connection rows.") Integer limit
    ) {
        if (routeId == null) {
            return new TransferContext(null, "Route id is required. Use findRoutes first when the admin mentions route number, name, or stations.", List.of());
        }

        return inTransaction(() -> {
            Instant from = parseInstant(dateFrom, Instant.now());
            Instant to = parseInstant(dateTo, from.plus(7, ChronoUnit.DAYS));
            int normalizedLimit = normalizeLimit(limit);

            List<Trip> sourceTrips = tripRepository.findTripsInPeriod(from, to, routeId, PageRequest.of(0, normalizedLimit));
            List<Trip> candidateTrips = tripRepository.findTripsInPeriod(from.minus(TRANSFER_WINDOW), to.plus(TRANSFER_WINDOW), null, PageRequest.of(0, MAX_TOOL_LIMIT));

            List<ConnectionContext> connections = sourceTrips.stream()
                    .flatMap(sourceTrip -> sortedStops(sourceTrip).stream()
                            .flatMap(sourceStop -> candidateTrips.stream()
                                    .filter(candidateTrip -> !candidateTrip.getId().equals(sourceTrip.getId()))
                                    .flatMap(candidateTrip -> sortedStops(candidateTrip).stream()
                                            .filter(candidateStop -> sameStation(sourceStop, candidateStop))
                                            .map(candidateStop -> toConnection(sourceTrip, sourceStop, candidateTrip, candidateStop)))))
                    .filter(connection -> Math.abs(connection.waitMinutes()) <= TRANSFER_WINDOW.toMinutes())
                    .sorted(Comparator.comparing(ConnectionContext::sourceArrivalAtUtc)
                            .thenComparing(connection -> Math.abs(connection.waitMinutes())))
                    .limit(normalizedLimit)
                    .toList();

            String note = connections.isEmpty()
                    ? "No nearby transfers were found in the selected period."
                    : "waitMinutes is negative when the connecting train departs before the analyzed route arrives.";
            return new TransferContext(routeId, note, connections);
        });
    }

    private TripContext toTripContext(Trip trip, TripRepository.TripCapacityRow capacity) {
        List<TripStopContext> stops = sortedStops(trip).stream()
                .map(stop -> new TripStopContext(
                        stop.getRouteStop().getStation().getId(),
                        stop.getRouteStop().getStation().getName(),
                        stop.getRouteStop().getOrdinal(),
                        trip.getDepartureAtUtc().plus(stop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES).toString(),
                        trip.getDepartureAtUtc().plus(stop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES).toString()
                ))
                .toList();
        long carCount = capacity == null ? countCars(trip) : value(capacity.getCarCount());
        long totalSeats = capacity == null ? countSeats(trip) : value(capacity.getTotalSeatCount());
        long soldSeats = capacity == null ? 0 : value(capacity.getSoldSeatCount());

        return new TripContext(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                trip.getDepartureAtUtc().toString(),
                carCount,
                totalSeats,
                soldSeats,
                occupancyPercent(soldSeats, totalSeats),
                stops
        );
    }

    private RouteContext toRouteContext(Route route) {
        List<RouteStopContext> stops = routeStopRepository.findByRouteId(route.getId())
                .stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .map(stop -> new RouteStopContext(
                        stop.getStation().getId(),
                        stop.getStation().getName(),
                        stop.getStation().getCountry().getCode(),
                        stop.getStation().getTimezone(),
                        stop.getOrdinal()
                ))
                .toList();

        return new RouteContext(
                route.getId(),
                route.getNumber(),
                route.getName(),
                route.getBasePrice(),
                route.getCurrency().getCode(),
                stops
        );
    }

    private String routeStopsText(Long routeId) {
        return routeStopRepository.findByRouteId(routeId).stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .map(stop -> stop.getStation().getName())
                .reduce((first, second) -> first + " -> " + second)
                .orElse("no stops");
    }

    private List<Route> findRoutesForQuery(String query) {
        if (query.isBlank()) {
            return routeRepository.findAll();
        }

        Map<Long, Route> routesById = new LinkedHashMap<>();
        List<String> parts = List.of(query.split("\\s+-\\s+"));
        if (!parts.isEmpty()) {
            routeRepository.findByNumberIgnoreCase(parts.getFirst().trim())
                    .ifPresent(route -> routesById.put(route.getId(), route));
        }

        routeRepository.searchForAdminAssistant(query)
                .forEach(route -> routesById.put(route.getId(), route));

        for (String part : parts) {
            String term = part.trim();
            if (!term.isBlank()) {
                routeRepository.searchForAdminAssistant(term)
                        .forEach(route -> routesById.put(route.getId(), route));
            }
        }

        return List.copyOf(routesById.values());
    }

    private ConnectionContext toConnection(Trip sourceTrip, TripStop sourceStop, Trip candidateTrip, TripStop candidateStop) {
        Instant sourceArrivalAt = sourceTrip.getDepartureAtUtc().plus(sourceStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES);
        Instant candidateDepartureAt = candidateTrip.getDepartureAtUtc().plus(candidateStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);

        return new ConnectionContext(
                sourceTrip.getId(),
                sourceTrip.getRoute().getId(),
                sourceTrip.getRoute().getNumber(),
                candidateTrip.getId(),
                candidateTrip.getRoute().getId(),
                candidateTrip.getRoute().getNumber(),
                sourceStop.getRouteStop().getStation().getName(),
                sourceArrivalAt.toString(),
                candidateDepartureAt.toString(),
                Duration.between(sourceArrivalAt, candidateDepartureAt).toMinutes()
        );
    }

    private Map<Long, TripRepository.TripCapacityRow> getCapacityByTripId(List<Trip> trips) {
        List<Long> tripIds = trips.stream().map(Trip::getId).toList();
        if (tripIds.isEmpty()) {
            return Map.of();
        }

        return tripRepository.findCapacityByTripIds(tripIds, Order.Status.PAID, SOLD_TICKET_STATUSES)
                .stream()
                .collect(Collectors.toMap(TripRepository.TripCapacityRow::getTripId, Function.identity()));
    }

    private List<TripStop> sortedStops(Trip trip) {
        return trip.getTripStops().stream()
                .sorted(Comparator.comparing(stop -> stop.getRouteStop().getOrdinal()))
                .toList();
    }

    private boolean sameStation(TripStop first, TripStop second) {
        return Objects.equals(first.getRouteStop().getStation().getId(), second.getRouteStop().getStation().getId());
    }

    private <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    private Instant parseInstant(String value, Instant fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException exception) {
            return fallback;
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_TOOL_LIMIT));
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    private long countCars(Trip trip) {
        return trip.getTripCars() == null ? 0 : trip.getTripCars().size();
    }

    private long countSeats(Trip trip) {
        if (trip.getTripCars() == null) {
            return 0;
        }
        return trip.getTripCars().stream()
                .mapToLong(car -> car.getTripSeats() == null ? 0 : car.getTripSeats().size())
                .sum();
    }

    private double occupancyPercent(long soldSeats, long totalSeats) {
        if (totalSeats == 0) {
            return 0.0;
        }
        return BigDecimal.valueOf(soldSeats)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(totalSeats), 2, RoundingMode.HALF_UP)
                .doubleValue();
    }

    public record CurrentTimeInfo(String utcNow, String systemTimezone, String localNow) {
    }

    public record SelectedAnalyticsFilters(
            Instant dateFrom,
            Instant dateTo,
            Long routeId,
            Integer limit
    ) {
    }

    public record RouteContext(
            Long routeId,
            String routeNumber,
            String routeName,
            BigDecimal basePrice,
            String currencyCode,
            List<RouteStopContext> stops
    ) {
    }

    public record RouteStopContext(
            Long stationId,
            String stationName,
            String countryCode,
            String timezone,
            Integer ordinal
    ) {
    }

    public record TripContext(
            Long tripId,
            Long routeId,
            String routeNumber,
            String routeName,
            String trainNumber,
            String trainName,
            String departureAtUtc,
            long carCount,
            long totalSeatCount,
            long soldSeatCount,
            double occupancyPercent,
            List<TripStopContext> stops
    ) {
    }

    public record TripStopContext(
            Long stationId,
            String stationName,
            Integer ordinal,
            String arrivalAtUtc,
            String departureAtUtc
    ) {
    }

    public record TransferContext(
            Long routeId,
            String note,
            List<ConnectionContext> connections
    ) {
    }

    public record ConnectionContext(
            Long sourceTripId,
            Long sourceRouteId,
            String sourceRouteNumber,
            Long connectingTripId,
            Long connectingRouteId,
            String connectingRouteNumber,
            String stationName,
            String sourceArrivalAtUtc,
            String connectingDepartureAtUtc,
            long waitMinutes
    ) {
    }
}
