package com.maksymchernenko.railwayticketsystem.service.admin;

import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsRequest;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.AdminAnalyticsResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.PopularSegmentResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.RouteRevenueResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.SalesTrendResponse;
import com.maksymchernenko.railwayticketsystem.dto.admin.analytics.TripOccupancyResponse;
import com.maksymchernenko.railwayticketsystem.dto.ai.context.CurrentTimeInfo;
import com.maksymchernenko.railwayticketsystem.dto.ai.context.RouteContext;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TrainRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.service.ai.context.AssistantCatalogService;
import com.maksymchernenko.railwayticketsystem.service.ai.context.AssistantToolSupport;
import com.maksymchernenko.railwayticketsystem.service.common.AnalyticsMath;
import dev.langchain4j.agent.tool.P;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class AdminAnalyticsTools {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;
    private static final int MAX_TRANSFER_TIMETABLE_TRIPS = 600;
    private static final Duration MIN_TRANSFER_WAIT = Duration.ofMinutes(25);
    private static final Duration MAX_TRANSFER_WAIT = Duration.ofHours(5);
    private static final Duration GOOD_TRANSFER_WAIT = Duration.ofMinutes(90);
    private static final double DIRECT_DEMAND_SUPPRESSION_RATIO = 0.35;
    private static final DateTimeFormatter LOCAL_DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final List<Ticket.Status> SOLD_TICKET_STATUSES = List.of(Ticket.Status.ACTIVE, Ticket.Status.FINISHED);

    private final AdminAnalyticsService analyticsService;
    private final TicketRepository ticketRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final StationRepository stationRepository;
    private final TrainRepository trainRepository;
    private final TripRepository tripRepository;
    private final AssistantCatalogService catalogService;
    private final AssistantToolSupport support;
    private final AnalyticsMath analyticsMath;
    private final TransactionTemplate transactionTemplate;
    private final ThreadLocal<SelectedAnalyticsFilters> selectedFilters = new ThreadLocal<>();

    public <T> T withSelectedAnalyticsFilters(SelectedAnalyticsFilters filters, Supplier<T> action) {
        selectedFilters.set(filters);
        try {
            return action.get();
        } finally {
            selectedFilters.remove();
        }
    }

    @Tool("Returns current backend time in UTC and application timezone.")
    public CurrentTimeInfo getCurrentSystemTime() {
        return support.currentTime();
    }

    @Tool("Returns analytics for filters currently selected in the admin analytics UI.")
    public AdminAnalyticsResponse getSelectedAnalyticsOverview() {
        SelectedAnalyticsFilters filters = selectedFilters.get();
        if (filters == null) {
            throw new IllegalStateException("Selected analytics filters are not available.");
        }
        return analytics(filters.dateFrom(), filters.dateTo(), filters.routeId(), filters.limit());
    }

    @Tool("Returns a compact admin-safe snapshot of system records without personal customer/order data.")
    public SystemRecordSummary getSystemRecordSummary() {
        return inTx(() -> {
            Instant now = Instant.now();
            return new SystemRecordSummary(
                    routeRepository.count(),
                    stationRepository.count(),
                    trainRepository.count(),
                    tripRepository.count(),
                    !tripRepository.findTripsInPeriod(now, now.plus(14, ChronoUnit.DAYS), null, PageRequest.of(0, 1)).isEmpty()
            );
        });
    }

    @Tool("Returns analytics for an explicitly requested period and optional route.")
    public AdminAnalyticsResponse getAnalyticsOverview(
            @P("ISO-8601 start instant, inclusive.") String dateFrom,
            @P("ISO-8601 end instant, exclusive.") String dateTo,
            @P("Optional route id. Use null for all routes.") Long routeId,
            @P("Maximum rows per analytics list.") Integer limit
    ) {
        Instant fallbackTo = Instant.now();
        Instant fallbackFrom = fallbackTo.minus(30, ChronoUnit.DAYS);
        return analytics(
                support.parseInstant(dateFrom, fallbackFrom),
                support.parseInstant(dateTo, fallbackTo),
                routeId,
                limit
        );
    }

    @Tool("Finds routes by number, name, or station text and returns ordered stops.")
    public List<RouteContext> findRoutes(
            @P("Route number, route name, station name, or station pair.") String query,
            @P("Maximum number of routes.") Integer limit
    ) {
        return inTx(() -> searchRoutes(query).stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(limit(limit))
                .map(catalogService::toRouteContext)
                .toList());
    }

    @Tool("Returns route network with ordered stops. Use for missing direct routes and route-extension ideas.")
    public List<RouteContext> getRouteNetwork(@P("Maximum number of routes.") Integer limit) {
        return catalogService.getRouteNetwork(limit(limit));
    }

    @Tool("Returns upcoming trip load, car count, seats, sold seats, and occupancy. Use for adding/removing cars or extra trips.")
    public List<TripLoad> getUpcomingTripLoad(
            @P("ISO-8601 start instant, inclusive. Null means now.") String dateFrom,
            @P("ISO-8601 end instant, exclusive. Null means 14 days from start.") String dateTo,
            @P("Optional route id. Use null for all routes.") Long routeId,
            @P("Maximum trips.") Integer limit
    ) {
        return inTx(() -> {
            Instant from = support.parseInstant(dateFrom, Instant.now());
            Instant to = support.parseInstant(dateTo, from.plus(14, ChronoUnit.DAYS));
            return tripRepository.findHighestOccupancyTrips(
                            from,
                            to,
                            routeId,
                            Order.Status.PAID,
                            SOLD_TICKET_STATUSES,
                            PageRequest.of(0, limit(limit))
                    )
                    .stream()
                    .map(row -> new TripLoad(
                            row.getTripId(),
                            row.getRouteId(),
                            row.getRouteNumber(),
                            row.getRouteName(),
                            row.getDepartureAtUtc().toString(),
                            displayTime(row.getDepartureAtUtc()),
                            value(row.getCarCount()),
                            value(row.getTotalSeatCount()),
                            value(row.getSoldSeatCount()),
                            analyticsMath.percent(value(row.getSoldSeatCount()), value(row.getTotalSeatCount()))
                    ))
                    .toList();
        });
    }

    @Tool("Returns service-improvement context for current selected filters: route demand, revenue, occupancy extremes, popular segments, and sales trend.")
    public ImprovementContext getServiceImprovementContext() {
        AdminAnalyticsResponse analytics = getSelectedAnalyticsOverview();
        return new ImprovementContext(
                analytics.demandByRoute(),
                analytics.revenueByRoute(),
                analytics.highSeatOccupancy(),
                analytics.lowSeatOccupancy(),
                analytics.popularSegments(),
                analytics.salesTrend(),
                recommendationRules()
        );
    }

    @Tool("Returns planning candidates for transfer corridors A-B-C. Demand comes from sold-ticket segments in the analytics period. Timetable connections are checked against upcoming trips from current backend time. Candidates with already-strong direct A-C demand are suppressed. Do not present planningDemandScore as exact transfer passengers.")
    public List<TransferDemand> getTransferDemandContext(
            @P("ISO-8601 start instant, inclusive. Use about 30 days ago when the admin asks for the last month. Null means last 30 days.") String dateFrom,
            @P("ISO-8601 end instant, exclusive. Use current backend time when the admin asks for the last month. Null means now.") String dateTo,
            @P("Maximum transfer chains.") Integer limit
    ) {
        Instant now = Instant.now();
        Instant demandFrom = support.parseInstant(dateFrom, now.minus(30, ChronoUnit.DAYS));
        Instant demandTo = support.parseInstant(dateTo, now);
        int normalizedLimit = Math.min(limit(limit), 5);

        return inTx(() -> {
            List<DemandSegment> segments = demandSegments(demandFrom, demandTo, Math.max(normalizedLimit * 8, 40));
            Map<String, DemandSegment> demandByDirection = segments.stream()
                    .collect(Collectors.toMap(
                            segment -> directionKey(segment.originStationId(), segment.destinationStationId()),
                            segment -> segment,
                            (first, second) -> first,
                            LinkedHashMap::new
                    ));
            List<Trip> upcomingTrips = tripRepository.findTripsInPeriod(
                    now,
                    now.plus(30, ChronoUnit.DAYS),
                    null,
                    PageRequest.of(0, MAX_TRANSFER_TIMETABLE_TRIPS)
            );

            List<TransferDemand> candidates = new ArrayList<>();
            for (DemandSegment first : segments) {
                for (DemandSegment second : segments) {
                    if (!Objects.equals(first.destinationStationId(), second.originStationId())
                            || Objects.equals(first.originStationId(), second.destinationStationId())) {
                        continue;
                    }

                    long score = Math.min(first.ticketCount(), second.ticketCount());
                    long directDemand = directDemand(first.originStationId(), second.destinationStationId(), demandByDirection);
                    if (directDemand >= Math.ceil(score * DIRECT_DEMAND_SUPPRESSION_RATIO)) {
                        continue;
                    }

                    TransferConnection connection = bestConnection(first, second, upcomingTrips);
                    if (connection != null) {
                        candidates.add(transferDemand(first, second, directDemand, score, connection));
                    }
                }
            }

            candidates = candidates.stream()
                    .sorted(transferDemandComparator())
                    .toList();

            Map<String, TransferDemand> uniqueByTarget = new LinkedHashMap<>();
            Map<String, Integer> suppressedByTarget = new LinkedHashMap<>();
            for (TransferDemand candidate : candidates) {
                String key = directionNameKey(candidate.origin(), candidate.destination());
                if (uniqueByTarget.containsKey(key)) {
                    suppressedByTarget.merge(key, 1, Integer::sum);
                } else {
                    uniqueByTarget.put(key, candidate);
                }
            }

            return uniqueByTarget.entrySet().stream()
                    .map(entry -> entry.getValue().withSuppressedSimilarCorridors(suppressedByTarget.getOrDefault(entry.getKey(), 0)))
                    .limit(normalizedLimit)
                    .toList();
        });
    }

    @Tool("Returns scheduled connections at a station for a period. waitMinutes outside 25-300 usually means a poor connection.")
    public List<ConnectionWindow> getConnectionWindows(
            @P("Station name used as the transfer point.") String stationName,
            @P("ISO-8601 start instant, inclusive.") String dateFrom,
            @P("ISO-8601 end instant, exclusive.") String dateTo,
            @P("Maximum rows.") Integer limit
    ) {
        return inTx(() -> {
            Instant from = support.parseInstant(dateFrom, Instant.now());
            Instant to = support.parseInstant(dateTo, from.plus(14, ChronoUnit.DAYS));
            List<Trip> trips = tripRepository.findTripsInPeriod(from, to.plus(MAX_TRANSFER_WAIT), null, PageRequest.of(0, MAX_LIMIT * 2));

            return trips.stream()
                    .flatMap(first -> stopAt(first, stationName).stream()
                            .flatMap(firstStop -> trips.stream()
                                    .filter(second -> !Objects.equals(first.getId(), second.getId()))
                                    .flatMap(second -> stopAt(second, stationName).stream()
                                            .map(secondStop -> connection(first, firstStop, second, secondStop)))))
                    .filter(connection -> connection.waitMinutes() >= -MAX_TRANSFER_WAIT.toMinutes())
                    .filter(connection -> connection.waitMinutes() <= MAX_TRANSFER_WAIT.toMinutes())
                    .sorted(Comparator.comparing(ConnectionWindow::firstArrivalAtUtc)
                            .thenComparing(connection -> Math.abs(connection.waitMinutes())))
                    .limit(limit(limit))
                    .toList();
        });
    }

    private AdminAnalyticsResponse analytics(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
        return inTx(() -> analyticsService.getAnalytics(new AdminAnalyticsRequest(dateFrom, dateTo, routeId, limit(limit))));
    }

    private List<Route> searchRoutes(String query) {
        if (!StringUtils.hasText(query)) {
            return routeRepository.findAll();
        }

        Map<Long, Route> routes = new LinkedHashMap<>();
        String normalized = query.trim();
        routeRepository.findByNumberIgnoreCase(normalized).ifPresent(route -> routes.put(route.getId(), route));
        routeRepository.searchForAdminAssistant(normalized).forEach(route -> routes.put(route.getId(), route));
        Stream.of(normalized.split("\\s+|,|-|\\u2013|\\u2014"))
                .map(String::trim)
                .filter(part -> part.length() >= 2)
                .forEach(part -> routeRepository.searchForAdminAssistant(part).forEach(route -> routes.put(route.getId(), route)));
        return List.copyOf(routes.values());
    }

    private List<DemandSegment> demandSegments(Instant dateFrom, Instant dateTo, int rows) {
        return ticketRepository.findPopularSegments(
                        dateFrom,
                        dateTo,
                        null,
                        Order.Status.PAID,
                        SOLD_TICKET_STATUSES,
                        PageRequest.of(0, rows)
                )
                .stream()
                .map(row -> new DemandSegment(
                        row.getStartStationId(),
                        row.getStartStationName(),
                        row.getEndStationId(),
                        row.getEndStationName(),
                        value(row.getTicketCount())
                ))
                .toList();
    }

    private TransferDemand transferDemand(
            DemandSegment first,
            DemandSegment second,
            long directDemand,
            long planningScore,
            TransferConnection connection
    ) {
        String directRouteIdea = "%s - %s".formatted(first.originStationName(), second.destinationStationName());
        List<String> existingDirectRoutes = directRouteNumbers(first.originStationName(), second.destinationStationName());
        String connectionQuality = connectionQuality(connection.waitMinutes());
        return new TransferDemand(
                first.originStationName(),
                first.destinationStationName(),
                second.destinationStationName(),
                first.ticketCount(),
                second.ticketCount(),
                directDemand,
                planningScore,
                connection.waitMinutes(),
                connectionQuality,
                directRouteIdea,
                !existingDirectRoutes.isEmpty(),
                existingDirectRoutes,
                connection,
                actionIdeas(first, second, directRouteIdea, existingDirectRoutes, directDemand, connection.waitMinutes(), connectionQuality),
                0,
                "The corridor was selected because both connected parts sold well in the selected period, the direct direction is not yet strongly represented, and the timetable contains a usable upcoming connection."
        );
    }

    private Comparator<TransferDemand> transferDemandComparator() {
        return Comparator
                .comparing(TransferDemand::directRouteAlreadyExists)
                .thenComparing(Comparator.comparingLong(TransferDemand::planningDemandScore).reversed())
                .thenComparingLong(TransferDemand::bestWaitMinutes);
    }

    private long directDemand(Long originStationId, Long destinationStationId, Map<String, DemandSegment> demandByDirection) {
        DemandSegment direct = demandByDirection.get(directionKey(originStationId, destinationStationId));
        return direct == null ? 0 : direct.ticketCount();
    }

    private String connectionQuality(long waitMinutes) {
        if (waitMinutes <= GOOD_TRANSFER_WAIT.toMinutes()) {
            return "good";
        }
        return "too_long";
    }

    private List<String> actionIdeas(
            DemandSegment first,
            DemandSegment second,
            String directRouteIdea,
            List<String> existingDirectRoutes,
            long directDemand,
            long bestWaitMinutes,
            String connectionQuality
    ) {
        String transferStation = first.destinationStationName();
        String secondLeg = "%s - %s".formatted(second.originStationName(), second.destinationStationName());
        List<String> ideas = new ArrayList<>();

        if (existingDirectRoutes.isEmpty()) {
            ideas.add("Consider a direct %s route because passengers already buy both connected parts, while the direct direction is still weak."
                    .formatted(directRouteIdea));
        } else {
            ideas.add("A direct %s route already exists (%s), so first review its visibility in search, timetable, capacity, and pricing."
                    .formatted(directRouteIdea, String.join(", ", existingDirectRoutes)));
        }

        if ("too_long".equals(connectionQuality)) {
            ideas.add("Move the %s departure closer to arrivals at %s; the current best connection requires about %d minutes of waiting, while a comfortable target is 25-90 minutes."
                    .formatted(secondLeg, transferStation, bestWaitMinutes));
        } else {
            ideas.add("Keep the %s connection visible in search and marketing; the current best transfer at %s takes about %d minutes."
                    .formatted(secondLeg, transferStation, bestWaitMinutes));
        }

        ideas.add("Before changing the timetable, check capacity and pricing on both parts of the journey.");
        return ideas;
    }

    private List<String> directRouteNumbers(String originStationName, String destinationStationName) {
        return routeRepository.findAll().stream()
                .filter(route -> containsOrderedStations(route, originStationName, destinationStationName))
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(5)
                .map(Route::getNumber)
                .toList();
    }

    private boolean containsOrderedStations(Route route, String originStationName, String destinationStationName) {
        List<RouteStop> stops = routeStopRepository.findByRouteId(route.getId()).stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .toList();
        int originOrdinal = -1;
        int destinationOrdinal = -1;
        for (RouteStop stop : stops) {
            String stationName = stop.getStation().getName();
            if (originOrdinal < 0 && stationName.equalsIgnoreCase(originStationName)) {
                originOrdinal = stop.getOrdinal();
            }
            if (stationName.equalsIgnoreCase(destinationStationName)) {
                destinationOrdinal = stop.getOrdinal();
            }
        }
        return originOrdinal >= 0 && destinationOrdinal > originOrdinal;
    }

    private TransferConnection bestConnection(DemandSegment first, DemandSegment second, List<Trip> trips) {
        return trips.stream()
                .flatMap(firstTrip -> segmentTimes(firstTrip, first.originStationId(), first.destinationStationId()).stream()
                        .flatMap(firstTimes -> trips.stream()
                                .filter(secondTrip -> !Objects.equals(firstTrip.getId(), secondTrip.getId()))
                                .flatMap(secondTrip -> segmentTimes(secondTrip, second.originStationId(), second.destinationStationId()).stream()
                                        .map(secondTimes -> connection(firstTimes, secondTimes)))))
                .filter(connection -> connection.waitMinutes() >= MIN_TRANSFER_WAIT.toMinutes())
                .filter(connection -> connection.waitMinutes() <= MAX_TRANSFER_WAIT.toMinutes())
                .min(Comparator.comparingLong(TransferConnection::waitMinutes))
                .orElse(null);
    }

    private TransferConnection connection(SegmentTimes first, SegmentTimes second) {
        long wait = Duration.between(first.arrivalAtUtc(), second.departureAtUtc()).toMinutes();
        return new TransferConnection(
                first.tripId(),
                first.routeNumber(),
                first.departureAtUtc().toString(),
                first.departureAtLocal(),
                first.arrivalAtUtc().toString(),
                first.arrivalAtLocal(),
                second.tripId(),
                second.routeNumber(),
                second.departureAtUtc().toString(),
                second.departureAtLocal(),
                second.arrivalAtUtc().toString(),
                second.arrivalAtLocal(),
                wait
        );
    }

    private List<SegmentTimes> segmentTimes(Trip trip, Long originStationId, Long destinationStationId) {
        TripStop originStop = stopByStationId(trip, originStationId);
        TripStop destinationStop = stopByStationId(trip, destinationStationId);
        if (originStop == null || destinationStop == null
                || originStop.getDepartureOffsetMinutes() >= destinationStop.getArrivalOffsetMinutes()) {
            return List.of();
        }

        return List.of(new SegmentTimes(
                trip.getId(),
                trip.getRoute().getNumber(),
                trip.getDepartureAtUtc().plus(originStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES),
                displayTime(trip.getDepartureAtUtc().plus(originStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES)),
                trip.getDepartureAtUtc().plus(destinationStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES),
                displayTime(trip.getDepartureAtUtc().plus(destinationStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES))
        ));
    }

    private String displayTime(Instant instant) {
        return instant.atZone(ZoneId.systemDefault()).format(LOCAL_DATE_TIME);
    }

    private TripStop stopByStationId(Trip trip, Long stationId) {
        return trip.getTripStops().stream()
                .filter(stop -> Objects.equals(stop.getRouteStop().getStation().getId(), stationId))
                .findFirst()
                .orElse(null);
    }

    private String directionKey(Long originStationId, Long destinationStationId) {
        return originStationId + "->" + destinationStationId;
    }

    private String directionNameKey(String origin, String destination) {
        return normalizeName(origin) + "->" + normalizeName(destination);
    }

    private String normalizeName(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    private List<TripStop> stopAt(Trip trip, String stationName) {
        if (!StringUtils.hasText(stationName)) {
            return List.of();
        }
        return trip.getTripStops().stream()
                .filter(stop -> stop.getRouteStop().getStation().getName().equalsIgnoreCase(stationName.trim()))
                .toList();
    }

    private ConnectionWindow connection(Trip first, TripStop firstStop, Trip second, TripStop secondStop) {
        Instant arrival = first.getDepartureAtUtc().plus(firstStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES);
        Instant departure = second.getDepartureAtUtc().plus(secondStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);
        long wait = Duration.between(arrival, departure).toMinutes();
        return new ConnectionWindow(
                first.getRoute().getNumber(),
                second.getRoute().getNumber(),
                firstStop.getRouteStop().getStation().getName(),
                arrival.toString(),
                displayTime(arrival),
                departure.toString(),
                displayTime(departure),
                wait,
                wait >= MIN_TRANSFER_WAIT.toMinutes() && wait <= MAX_TRANSFER_WAIT.toMinutes()
        );
    }

    private List<String> recommendationRules() {
        return List.of(
                "High occupancy or strong demand: add cars, add another trip, or review price/availability.",
                "Low occupancy: reduce cars, test discounts, adjust departure time, or combine low-demand service.",
                "Popular adjacent sold segments A-B and B-C: for each corridor, suggest a direct A-C route or extra direct trip, and suggest how to improve B-C timing at B.",
                "Revenue without demand may indicate high prices; demand without revenue may indicate discount or capacity pressure.",
                "Use sales trend direction before claiming popularity is rising or falling."
        );
    }

    private <T> T inTx(Supplier<T> action) {
        return support.inTransaction(transactionTemplate, action);
    }

    private int limit(Integer value) {
        return support.limit(value, DEFAULT_LIMIT, MAX_LIMIT);
    }

    private long value(Long value) {
        return value == null ? 0 : value;
    }

    public record SelectedAnalyticsFilters(Instant dateFrom, Instant dateTo, Long routeId, Integer limit) {
    }

    public record TripLoad(
            Long tripId,
            Long routeId,
            String routeNumber,
            String routeName,
            String departureAtUtc,
            String departureAtLocal,
            long carCount,
            long totalSeatCount,
            long soldSeatCount,
            double occupancyPercent
    ) {
    }

    public record SystemRecordSummary(
            long routeCount,
            long stationCount,
            long trainCount,
            long tripCount,
            boolean hasUpcomingTripsInNext14Days
    ) {
    }

    public record ImprovementContext(
            List<RouteRevenueResponse> demandByRoute,
            List<RouteRevenueResponse> revenueByRoute,
            List<TripOccupancyResponse> highOccupancyTrips,
            List<TripOccupancyResponse> lowOccupancyTrips,
            List<PopularSegmentResponse> popularSegments,
            List<SalesTrendResponse> salesTrend,
            List<String> recommendationRules
    ) {
    }

    public record TransferDemand(
            String origin,
            String transferStation,
            String destination,
            long firstLegTickets,
            long secondLegTickets,
            long directDemandTickets,
            long planningDemandScore,
            long bestWaitMinutes,
            String connectionQuality,
            String directRouteIdea,
            boolean directRouteAlreadyExists,
            List<String> existingDirectRouteNumbers,
            TransferConnection connection,
            List<String> actionIdeas,
            int similarCorridorVariantsSuppressed,
            String evidenceNote
    ) {
        private TransferDemand withSuppressedSimilarCorridors(int count) {
            return new TransferDemand(
                    origin,
                    transferStation,
                    destination,
                    firstLegTickets,
                    secondLegTickets,
                    directDemandTickets,
                    planningDemandScore,
                    bestWaitMinutes,
                    connectionQuality,
                    directRouteIdea,
                    directRouteAlreadyExists,
                    existingDirectRouteNumbers,
                    connection,
                    actionIdeas,
                    count,
                    evidenceNote
            );
        }
    }

    private record DemandSegment(
            Long originStationId,
            String originStationName,
            Long destinationStationId,
            String destinationStationName,
            long ticketCount
    ) {
    }

    public record TransferConnection(
            Long firstTripId,
            String firstRouteNumber,
            String firstDepartureAtUtc,
            String firstDepartureAtLocal,
            String firstArrivalAtUtc,
            String firstArrivalAtTransferLocal,
            Long secondTripId,
            String secondRouteNumber,
            String secondDepartureAtUtc,
            String secondDepartureAtTransferLocal,
            String secondArrivalAtUtc,
            String secondArrivalAtLocal,
            long waitMinutes
    ) {
    }

    private record SegmentTimes(
            Long tripId,
            String routeNumber,
            Instant departureAtUtc,
            String departureAtLocal,
            Instant arrivalAtUtc,
            String arrivalAtLocal
    ) {
    }

    public record ConnectionWindow(
            String firstRouteNumber,
            String secondRouteNumber,
            String transferStation,
            String firstArrivalAtUtc,
            String firstArrivalAtLocal,
            String secondDepartureAtUtc,
            String secondDepartureAtLocal,
            long waitMinutes,
            boolean comfortableConnection
    ) {
    }
}
