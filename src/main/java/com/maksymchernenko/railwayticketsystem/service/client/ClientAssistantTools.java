package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.BenefitRepository;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class ClientAssistantTools {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_TOOL_LIMIT = 40;
    private static final int MAX_TRANSFER_TRIPS = 120;
    private static final Duration MIN_TRANSFER_WAIT = Duration.ofMinutes(30);
    private static final Duration MAX_TRANSFER_WAIT = Duration.ofHours(6);

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final TripRepository tripRepository;
    private final TicketRepository ticketRepository;
    private final BenefitRepository benefitRepository;
    private final TransactionTemplate transactionTemplate;

    @Tool("Returns the current backend server time in UTC and in the application timezone. Use it before answering about nearest, today, tomorrow, or upcoming trips.")
    public CurrentTimeInfo getCurrentSystemTime() {
        Instant now = Instant.now();
        ZoneId zoneId = ZoneId.systemDefault();
        return new CurrentTimeInfo(now.toString(), zoneId.toString(), now.atZone(zoneId).toString());
    }

    @Tool("Finds passenger stations by city/station name, country code, or address. Use it when the client names a city or station.")
    public List<StationContext> findStations(
            @P("City, station name, country code, or address fragment from the client's message.") String query,
            @P("Maximum number of stations to return.") Integer limit
    ) {
        return inTransaction(() -> searchStations(query).stream()
                .limit(normalizeLimit(limit))
                .map(this::toStationContext)
                .toList());
    }

    @Tool("Finds routes by route number, route name, or station names and returns ordered stops. Use it to explain available directions or missing direct routes.")
    public List<RouteContext> findRoutes(
            @P("Route number, route name, station name, or route fragment from the client's request.") String query,
            @P("Maximum number of routes to return.") Integer limit
    ) {
        return inTransaction(() -> findRoutesForQuery(query).stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(normalizeLimit(limit))
                .map(this::toRouteContext)
                .toList());
    }

    @Tool("Searches direct upcoming trips between two station/city names and returns practical ticket options with price and available seat count. Use this for trip, ticket, price, nearest, morning/evening/night, or cheapest direct-trip questions.")
    public TripSearchContext searchDirectTrips(
            @P("Origin city or station name, for example Kharkiv.") String origin,
            @P("Destination city or station name, for example Kyiv.") String destination,
            @P("Optional start date/time. Accepts ISO instant like 2026-06-20T00:00:00Z or local date like 2026-06-20. Use null for now.") String dateFrom,
            @P("How many days to search from dateFrom. Use 1 for a specific date, 7-14 for nearest options.") Integer days,
            @P("Maximum number of trip options to return.") Integer limit
    ) {
        return inTransaction(() -> {
            ResolvedStations resolved = resolveStations(origin, destination);
            if (resolved.hasProblem()) {
                return TripSearchContext.withProblem(resolved.note(), resolved.originCandidates(), resolved.destinationCandidates());
            }

            Station originStation = resolved.origin();
            Station destinationStation = resolved.destination();
            Instant from = parseDateStart(dateFrom, originStation, Instant.now());
            Instant to = from.plus(Math.max(1, Math.min(days == null ? 14 : days, 30)), ChronoUnit.DAYS);

            List<TripOption> options = tripRepository
                    .findTripsContainingSegment(originStation.getId(), destinationStation.getId())
                    .stream()
                    .map(trip -> toTripOption(trip, originStation, destinationStation))
                    .flatMap(Optional::stream)
                    .filter(option -> !option.departureAtUtc().isBefore(from))
                    .filter(option -> option.departureAtUtc().isBefore(to))
                    .sorted(Comparator.comparing(TripOption::departureAtUtc)
                            .thenComparing(TripOption::basePrice))
                    .limit(normalizeLimit(limit))
                    .toList();

            String note = options.isEmpty()
                    ? "No direct upcoming trips were found for the selected stations and period."
                    : "Direct upcoming trips found. availableSeatCount is calculated for the requested segment.";

            return new TripSearchContext(
                    toStationContext(originStation),
                    toStationContext(destinationStation),
                    from.toString(),
                    to.toString(),
                    note,
                    options,
                    List.of(),
                    List.of()
            );
        });
    }

    @Tool("Finds one-transfer options between two station/city names when direct trips are missing or inconvenient. Use it for alternative route questions.")
    public TransferSearchContext findTransferOptions(
            @P("Origin city or station name.") String origin,
            @P("Destination city or station name.") String destination,
            @P("Optional start date/time. Accepts ISO instant or local date. Use null for now.") String dateFrom,
            @P("How many days to search from dateFrom. Use 7-14 for alternatives.") Integer days,
            @P("Maximum number of transfer options to return.") Integer limit
    ) {
        return inTransaction(() -> {
            ResolvedStations resolved = resolveStations(origin, destination);
            if (resolved.hasProblem()) {
                return TransferSearchContext.withProblem(resolved.note(), resolved.originCandidates(), resolved.destinationCandidates());
            }

            Station originStation = resolved.origin();
            Station destinationStation = resolved.destination();
            Instant from = parseDateStart(dateFrom, originStation, Instant.now());
            Instant to = from.plus(Math.max(1, Math.min(days == null ? 14 : days, 30)), ChronoUnit.DAYS);
            List<Trip> trips = tripRepository.findTripsInPeriod(from, to.plus(MAX_TRANSFER_WAIT), null, PageRequest.of(0, MAX_TRANSFER_TRIPS));
            List<TransferOption> options = new ArrayList<>();

            for (Trip firstTrip : trips) {
                TripStop originStop = findTripStopByStationId(firstTrip, originStation.getId());
                if (originStop == null) {
                    continue;
                }

                List<TripStop> possibleTransferStops = sortedStops(firstTrip).stream()
                        .filter(stop -> originStop.getDepartureOffsetMinutes() < stop.getArrivalOffsetMinutes())
                        .filter(stop -> !Objects.equals(stop.getRouteStop().getStation().getId(), destinationStation.getId()))
                        .toList();

                for (TripStop transferArrivalStop : possibleTransferStops) {
                    Station transferStation = transferArrivalStop.getRouteStop().getStation();
                    Instant firstArrival = firstTrip.getDepartureAtUtc().plus(transferArrivalStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES);

                    for (Trip secondTrip : trips) {
                        if (Objects.equals(firstTrip.getId(), secondTrip.getId())) {
                            continue;
                        }

                        TripStop transferDepartureStop = findTripStopByStationId(secondTrip, transferStation.getId());
                        TripStop destinationStop = findTripStopByStationId(secondTrip, destinationStation.getId());
                        if (transferDepartureStop == null || destinationStop == null
                                || transferDepartureStop.getDepartureOffsetMinutes() >= destinationStop.getArrivalOffsetMinutes()) {
                            continue;
                        }

                        Instant secondDeparture = secondTrip.getDepartureAtUtc().plus(transferDepartureStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);
                        long waitMinutes = Duration.between(firstArrival, secondDeparture).toMinutes();
                        if (waitMinutes < MIN_TRANSFER_WAIT.toMinutes() || waitMinutes > MAX_TRANSFER_WAIT.toMinutes()) {
                            continue;
                        }

                        toTripOption(firstTrip, originStation, transferStation)
                                .flatMap(first -> toTripOption(secondTrip, transferStation, destinationStation)
                                        .map(second -> new TransferOption(transferStation.getName(), waitMinutes, first, second)))
                                .ifPresent(options::add);
                    }
                }
            }

            List<TransferOption> limitedOptions = options.stream()
                    .sorted(Comparator.comparing(option -> option.firstTrip().departureAtUtc()))
                    .limit(normalizeLimit(limit))
                    .toList();
            String note = limitedOptions.isEmpty()
                    ? "No one-transfer options were found in the selected period."
                    : "One-transfer options found. Compare total duration, wait time, price, and available seats.";

            return new TransferSearchContext(
                    toStationContext(originStation),
                    toStationContext(destinationStation),
                    from.toString(),
                    to.toString(),
                    note,
                    limitedOptions,
                    List.of(),
                    List.of()
            );
        });
    }

    @Tool("Returns configured passenger benefits and discount percentages. Use it for questions about discounts, benefits, student or privileged tickets.")
    public List<BenefitContext> getAvailableBenefits() {
        return inTransaction(() -> benefitRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(benefit -> benefit.getCode().toLowerCase()))
                .map(benefit -> new BenefitContext(benefit.getCode(), benefit.getPercentAmount()))
                .toList());
    }

    @Tool("Returns the route network with ordered station stops. Use it when the client asks what directions are available or why a direct trip cannot be bought.")
    public List<RouteContext> getRouteNetwork(
            @P("Maximum number of routes to return.") Integer limit
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

    private ResolvedStations resolveStations(String origin, String destination) {
        List<Station> originCandidates = searchStations(origin);
        List<Station> destinationCandidates = searchStations(destination);

        if (originCandidates.isEmpty() || destinationCandidates.isEmpty()) {
            return ResolvedStations.problem(
                    "Could not resolve one or both stations from the client's message.",
                    originCandidates.stream().map(this::toStationContext).toList(),
                    destinationCandidates.stream().map(this::toStationContext).toList()
            );
        }

        Station originStation = bestStationMatch(origin, originCandidates);
        Station destinationStation = bestStationMatch(destination, destinationCandidates);
        if (Objects.equals(originStation.getId(), destinationStation.getId())) {
            return ResolvedStations.problem(
                    "Origin and destination resolve to the same station.",
                    originCandidates.stream().map(this::toStationContext).toList(),
                    destinationCandidates.stream().map(this::toStationContext).toList()
            );
        }

        return ResolvedStations.ok(originStation, destinationStation);
    }

    private List<Station> searchStations(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return stationRepository.findAll().stream()
                    .sorted(Comparator.comparing(Station::getName))
                    .toList();
        }

        Map<Long, Station> stationsById = new LinkedHashMap<>();
        stationRepository.searchForAssistant(normalizedQuery)
                .forEach(station -> stationsById.put(station.getId(), station));

        for (String part : normalizedQuery.split("\\s+|,|\\-|\\u2014")) {
            String term = part.trim();
            if (term.length() >= 2) {
                stationRepository.searchForAssistant(term)
                        .forEach(station -> stationsById.put(station.getId(), station));
            }
        }

        return List.copyOf(stationsById.values());
    }

    private Station bestStationMatch(String query, List<Station> candidates) {
        String normalizedQuery = query == null ? "" : query.trim();
        return candidates.stream()
                .filter(station -> station.getName().equalsIgnoreCase(normalizedQuery))
                .findFirst()
                .orElse(candidates.getFirst());
    }

    private List<Route> findRoutesForQuery(String query) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isBlank()) {
            return routeRepository.findAll();
        }

        Map<Long, Route> routesById = new LinkedHashMap<>();
        routeRepository.findByNumberIgnoreCase(normalizedQuery)
                .ifPresent(route -> routesById.put(route.getId(), route));
        routeRepository.searchForAdminAssistant(normalizedQuery)
                .forEach(route -> routesById.put(route.getId(), route));

        for (String part : normalizedQuery.split("\\s+|,|\\-|\\u2014")) {
            String term = part.trim();
            if (term.length() >= 2) {
                routeRepository.searchForAdminAssistant(term)
                        .forEach(route -> routesById.put(route.getId(), route));
            }
        }

        return List.copyOf(routesById.values());
    }

    private Optional<TripOption> toTripOption(Trip trip, Station originStation, Station destinationStation) {
        TripStop originStop = findTripStopByStationId(trip, originStation.getId());
        TripStop destinationStop = findTripStopByStationId(trip, destinationStation.getId());
        if (originStop == null || destinationStop == null
                || originStop.getDepartureOffsetMinutes() >= destinationStop.getArrivalOffsetMinutes()) {
            return Optional.empty();
        }

        Instant departureAt = trip.getDepartureAtUtc().plus(originStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);
        Instant arrivalAt = trip.getDepartureAtUtc().plus(destinationStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES);
        if (!departureAt.isAfter(Instant.now())) {
            return Optional.empty();
        }

        SeatAvailability availability = getSeatAvailability(trip, originStop, destinationStop);
        return Optional.of(new TripOption(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                originStop.getId(),
                originStation.getName(),
                departureAt,
                destinationStop.getId(),
                destinationStation.getName(),
                arrivalAt,
                trip.getRoute().getBasePrice(),
                trip.getRoute().getCurrency().getCode(),
                availability.totalSeatCount(),
                availability.availableSeatCount()
        ));
    }

    private SeatAvailability getSeatAvailability(Trip trip, TripStop originStop, TripStop destinationStop) {
        int totalSeats = trip.getTripCars().stream()
                .map(TripCar::getTripSeats)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
        Set<Integer> occupiedTripSeatIds = Set.copyOf(ticketRepository.findOccupiedTripSeatIdsForSegment(
                trip.getId(),
                originStop.getDepartureOffsetMinutes(),
                destinationStop.getArrivalOffsetMinutes(),
                Order.Status.PAID,
                Order.Status.PENDING,
                Instant.now(),
                Ticket.SEAT_BLOCKING_STATUSES
        ));

        return new SeatAvailability(totalSeats, Math.max(0, totalSeats - occupiedTripSeatIds.size()));
    }

    private TripStop findTripStopByStationId(Trip trip, Long stationId) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getRouteStop().getStation().getId().equals(stationId))
                .findFirst()
                .orElse(null);
    }

    private List<TripStop> sortedStops(Trip trip) {
        return trip.getTripStops().stream()
                .sorted(Comparator.comparing(stop -> stop.getRouteStop().getOrdinal()))
                .toList();
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

    private StationContext toStationContext(Station station) {
        return new StationContext(
                station.getId(),
                station.getName(),
                station.getCountry().getCode(),
                station.getTimezone(),
                station.getAccessible()
        );
    }

    private Instant parseDateStart(String value, Station station, Instant fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }

        try {
            return Instant.parse(value);
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim())
                        .atStartOfDay(ZoneId.of(station.getTimezone()))
                        .toInstant();
            } catch (DateTimeParseException secondIgnored) {
                return fallback;
            }
        }
    }

    private int normalizeLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(limit, MAX_TOOL_LIMIT));
    }

    private <T> T inTransaction(Supplier<T> action) {
        return transactionTemplate.execute(status -> action.get());
    }

    private record SeatAvailability(int totalSeatCount, int availableSeatCount) {
    }

    private record ResolvedStations(
            Station origin,
            Station destination,
            String note,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static ResolvedStations ok(Station origin, Station destination) {
            return new ResolvedStations(origin, destination, null, List.of(), List.of());
        }

        private static ResolvedStations problem(
                String note,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new ResolvedStations(null, null, note, originCandidates, destinationCandidates);
        }

        private boolean hasProblem() {
            return note != null;
        }
    }

    public record CurrentTimeInfo(String utcNow, String systemTimezone, String localNow) {
    }

    public record StationContext(
            Long stationId,
            String stationName,
            String countryCode,
            String timezone,
            Boolean accessible
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

    public record TripSearchContext(
            StationContext origin,
            StationContext destination,
            String dateFromUtc,
            String dateToUtc,
            String note,
            List<TripOption> directTrips,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static TripSearchContext withProblem(
                String note,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new TripSearchContext(null, null, null, null, note, List.of(), originCandidates, destinationCandidates);
        }
    }

    public record TransferSearchContext(
            StationContext origin,
            StationContext destination,
            String dateFromUtc,
            String dateToUtc,
            String note,
            List<TransferOption> transferOptions,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static TransferSearchContext withProblem(
                String note,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new TransferSearchContext(null, null, null, null, note, List.of(), originCandidates, destinationCandidates);
        }
    }

    public record TripOption(
            Long tripId,
            Long routeId,
            String routeNumber,
            String routeName,
            String trainNumber,
            String trainName,
            Long departureTripStopId,
            String departureStationName,
            Instant departureAtUtc,
            Long arrivalTripStopId,
            String arrivalStationName,
            Instant arrivalAtUtc,
            BigDecimal basePrice,
            String currencyCode,
            int totalSeatCount,
            int availableSeatCount
    ) {
    }

    public record TransferOption(
            String transferStationName,
            long waitMinutes,
            TripOption firstTrip,
            TripOption secondTrip
    ) {
    }

    public record BenefitContext(
            String code,
            BigDecimal percentAmount
    ) {
    }
}
