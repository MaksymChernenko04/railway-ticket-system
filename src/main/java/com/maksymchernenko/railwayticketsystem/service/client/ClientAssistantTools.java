package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.ai.context.CurrentTimeInfo;
import com.maksymchernenko.railwayticketsystem.dto.ai.context.RouteContext;
import com.maksymchernenko.railwayticketsystem.dto.ai.context.StationContext;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TrainRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.BenefitRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CarTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.SeatTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.TrainTypeRepository;
import com.maksymchernenko.railwayticketsystem.service.ai.context.AssistantCatalogService;
import com.maksymchernenko.railwayticketsystem.service.ai.context.AssistantToolSupport;
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
import java.time.format.DateTimeFormatter;
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
import java.util.stream.Stream;

@Component
@RequiredArgsConstructor
public class ClientAssistantTools {

    private static final int DEFAULT_LIMIT = 8;
    private static final int MAX_LIMIT = 40;
    private static final int MAX_TRANSFER_TRIPS = 120;
    private static final Duration MIN_TRANSFER_WAIT = Duration.ofMinutes(25);
    private static final Duration MAX_TRANSFER_WAIT = Duration.ofHours(6);
    private static final DateTimeFormatter PASSENGER_TIME_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final TripRepository tripRepository;
    private final TrainRepository trainRepository;
    private final TicketRepository ticketRepository;
    private final BenefitRepository benefitRepository;
    private final CarTypeRepository carTypeRepository;
    private final SeatTypeRepository seatTypeRepository;
    private final TrainTypeRepository trainTypeRepository;
    private final AssistantCatalogService catalogService;
    private final AssistantToolSupport support;
    private final TransactionTemplate transactionTemplate;

    @Tool("Returns current backend time in UTC and application timezone. Use for today, tomorrow, nearest, and upcoming trip questions.")
    public CurrentTimeInfo getCurrentSystemTime() {
        return support.currentTime();
    }

    @Tool("Finds passenger stations by city/station name, country code, or address.")
    public List<StationContext> findStations(
            @P("City, station name, country code, or address fragment.") String query,
            @P("Maximum stations.") Integer limit
    ) {
        return inTx(() -> searchStations(query).stream()
                .limit(limit(limit))
                .map(this::stationContext)
                .toList());
    }

    @Tool("Finds passenger routes by number, route name, or station names and returns ordered stops.")
    public List<RouteContext> findRoutes(
            @P("Route number, route name, station name, or station pair.") String query,
            @P("Maximum routes.") Integer limit
    ) {
        return inTx(() -> searchRoutes(query).stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(limit(limit))
                .map(catalogService::toRouteContext)
                .toList());
    }

    @Tool("Returns route network with ordered stops for available directions.")
    public List<RouteContext> getRouteNetwork(@P("Maximum routes.") Integer limit) {
        return catalogService.getRouteNetwork(limit(limit));
    }

    @Tool("Searches direct upcoming passenger trips between two stations/cities with prices and available seats. Use this before transfers.")
    public TripSearchResult searchDirectTrips(
            @P("Origin city or station name.") String origin,
            @P("Destination city or station name.") String destination,
            @P("Optional ISO instant or local date yyyy-MM-dd. Null means now.") String dateFrom,
            @P("How many days to search. Use 1 for an exact date, 7-14 for nearest options.") Integer days,
            @P("Maximum trip options.") Integer limit
    ) {
        return inTx(() -> {
            StationPair pair = resolvePair(origin, destination);
            if (pair.problem() != null) {
                return TripSearchResult.problem(pair.problem(), pair.originCandidates(), pair.destinationCandidates());
            }

            Instant from = parseDate(dateFrom, pair.origin(), Instant.now());
            Instant to = from.plus(Math.clamp(days == null ? 14 : days, 1, 30), ChronoUnit.DAYS);
            List<TripOption> options = tripRepository.findTripsContainingSegment(pair.origin().getId(), pair.destination().getId())
                    .stream()
                    .map(trip -> tripOption(trip, pair.origin(), pair.destination()))
                    .flatMap(Optional::stream)
                    .filter(option -> !option.departureAtUtc().isBefore(from) && option.departureAtUtc().isBefore(to))
                    .sorted(Comparator.comparing(TripOption::departureAtUtc).thenComparing(TripOption::basePrice))
                    .limit(limit(limit))
                    .toList();

            return new TripSearchResult(
                    stationContext(pair.origin()),
                    stationContext(pair.destination()),
                    from.toString(),
                    to.toString(),
                    options.isEmpty() ? "No direct trips found in this period." : "Direct trip options found.",
                    options,
                    List.of(),
                    List.of()
            );
        });
    }

    @Tool("Finds one-transfer passenger trip options when direct trips are missing or inconvenient.")
    public TransferSearchResult findTransferOptions(
            @P("Origin city or station name.") String origin,
            @P("Destination city or station name.") String destination,
            @P("Optional ISO instant or local date yyyy-MM-dd. Null means now.") String dateFrom,
            @P("How many days to search.") Integer days,
            @P("Maximum transfer options.") Integer limit
    ) {
        return inTx(() -> {
            StationPair pair = resolvePair(origin, destination);
            if (pair.problem() != null) {
                return TransferSearchResult.problem(pair.problem(), pair.originCandidates(), pair.destinationCandidates());
            }

            Instant from = parseDate(dateFrom, pair.origin(), Instant.now());
            Instant to = from.plus(Math.clamp(days == null ? 14 : days, 1, 30), ChronoUnit.DAYS);
            List<Trip> trips = tripRepository.findTripsInPeriod(from, to.plus(MAX_TRANSFER_WAIT), null, PageRequest.of(0, MAX_TRANSFER_TRIPS));
            List<TransferOption> options = new ArrayList<>();

            for (Trip firstTrip : trips) {
                TripStop originStop = stop(firstTrip, pair.origin().getId());
                if (originStop == null) {
                    continue;
                }
                Instant firstDeparture = tripInstant(firstTrip, originStop.getDepartureOffsetMinutes());
                if (firstDeparture.isBefore(from) || !firstDeparture.isBefore(to)) {
                    continue;
                }
                for (TripStop transferArrivalStop : laterStops(firstTrip, originStop)) {
                    Station transferStation = transferArrivalStop.getRouteStop().getStation();
                    if (Objects.equals(transferStation.getId(), pair.destination().getId())) {
                        continue;
                    }
                    Instant transferArrival = tripInstant(firstTrip, transferArrivalStop.getArrivalOffsetMinutes());

                    for (Trip secondTrip : trips) {
                        if (Objects.equals(firstTrip.getId(), secondTrip.getId())) {
                            continue;
                        }
                        TripStop transferDepartureStop = stop(secondTrip, transferStation.getId());
                        TripStop destinationStop = stop(secondTrip, pair.destination().getId());
                        if (transferDepartureStop == null || destinationStop == null
                                || transferDepartureStop.getDepartureOffsetMinutes() >= destinationStop.getArrivalOffsetMinutes()) {
                            continue;
                        }

                        Instant transferDeparture = tripInstant(secondTrip, transferDepartureStop.getDepartureOffsetMinutes());
                        long waitMinutes = Duration.between(transferArrival, transferDeparture).toMinutes();
                        if (waitMinutes < MIN_TRANSFER_WAIT.toMinutes() || waitMinutes > MAX_TRANSFER_WAIT.toMinutes()) {
                            continue;
                        }

                        Optional<TripOption> first = tripOption(firstTrip, pair.origin(), transferStation);
                        Optional<TripOption> second = tripOption(secondTrip, transferStation, pair.destination());
                        if (first.isPresent() && second.isPresent()) {
                            options.add(new TransferOption(transferStation.getName(), waitMinutes, first.get(), second.get()));
                        }
                    }
                }
            }

            List<TransferOption> limited = options.stream()
                    .sorted(Comparator.comparing(option -> option.firstTrip().departureAtUtc()))
                    .limit(limit(limit))
                    .toList();
            return new TransferSearchResult(
                    stationContext(pair.origin()),
                    stationContext(pair.destination()),
                    from.toString(),
                    to.toString(),
                    limited.isEmpty() ? "No one-transfer options found in this period." : "One-transfer options found.",
                    limited,
                    List.of(),
                    List.of()
            );
        });
    }

    @Tool("Returns passenger-safe reference data: discounts, train types, car types, seat types, and trains.")
    public PassengerReferenceData getPassengerReferenceData(@P("Maximum trains to return.") Integer limit) {
        return inTx(() -> new PassengerReferenceData(
                benefitRepository.findAll().stream()
                        .sorted(Comparator.comparing(benefit -> benefit.getCode().toLowerCase()))
                        .map(benefit -> new BenefitInfo(benefit.getCode(), benefit.getPercentAmount()))
                        .toList(),
                trainTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(type -> type.getCode().toLowerCase()))
                        .map(type -> new PricedTypeInfo(type.getCode(), type.getPriceMultiplier(), "Train type price multiplier. No separate description is configured."))
                        .toList(),
                carTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(type -> type.getCode().toLowerCase()))
                        .map(type -> new PricedTypeInfo(type.getCode(), type.getPriceMultiplier(), "Car type price multiplier. No separate description is configured."))
                        .toList(),
                seatTypeRepository.findAll().stream()
                        .sorted(Comparator.comparing(type -> type.getCode().toLowerCase()))
                        .map(type -> new PricedTypeInfo(type.getCode(), type.getPriceMultiplier(), "Seat type price multiplier. No separate description is configured."))
                        .toList(),
                trainRepository.findAll().stream()
                        .sorted(Comparator.comparing(Train::getNumber))
                        .limit(limit(limit))
                        .map(train -> new TrainInfo(train.getId(), train.getNumber(), train.getName(), train.getType().getCode()))
                        .toList()
        ));
    }

    @Tool("Returns cars and seat counts for a passenger trip. Use after a trip was selected from searchDirectTrips.")
    public List<TripCarInfo> getTripCarsAndSeats(@P("Trip id from searchDirectTrips.") Long tripId) {
        if (tripId == null) {
            return List.of();
        }
        return inTx(() -> tripRepository.findById(tripId)
                .stream()
                .flatMap(trip -> trip.getTripCars().stream())
                .sorted(Comparator.comparing(TripCar::getOrdinal))
                .map(tripCar -> new TripCarInfo(
                        tripCar.getOrdinal(),
                        tripCar.getCar().getNumber(),
                        tripCar.getCar().getType().getCode(),
                        tripCar.getCar().getAccessible(),
                        tripCar.getTripSeats() == null ? 0 : tripCar.getTripSeats().size()
                ))
                .toList());
    }

    private StationPair resolvePair(String origin, String destination) {
        List<Station> origins = searchStations(origin);
        List<Station> destinations = searchStations(destination);
        if (origins.isEmpty() || destinations.isEmpty()) {
            return StationPair.problem(
                    "Could not resolve origin or destination station.",
                    origins.stream().map(this::stationContext).toList(),
                    destinations.stream().map(this::stationContext).toList()
            );
        }
        Station start = origins.getFirst();
        Station end = destinations.getFirst();
        if (Objects.equals(start.getId(), end.getId())) {
            return StationPair.problem(
                    "Origin and destination are the same station.",
                    origins.stream().map(this::stationContext).toList(),
                    destinations.stream().map(this::stationContext).toList()
            );
        }
        return StationPair.ok(start, end);
    }

    private List<Station> searchStations(String query) {
        if (!StringUtils.hasText(query)) {
            return stationRepository.findAll().stream()
                    .sorted(Comparator.comparing(Station::getName))
                    .toList();
        }

        Map<Long, Station> stations = new LinkedHashMap<>();
        String normalized = query.trim();
        stationRepository.searchForAssistant(normalized).forEach(station -> stations.put(station.getId(), station));
        tokens(normalized).forEach(token -> stationRepository.searchForAssistant(token).forEach(station -> stations.put(station.getId(), station)));
        return List.copyOf(stations.values());
    }

    private List<Route> searchRoutes(String query) {
        if (!StringUtils.hasText(query)) {
            return routeRepository.findAll();
        }

        Map<Long, Route> routes = new LinkedHashMap<>();
        String normalized = query.trim();
        routeRepository.findByNumberIgnoreCase(normalized).ifPresent(route -> routes.put(route.getId(), route));
        routeRepository.searchForAdminAssistant(normalized).forEach(route -> routes.put(route.getId(), route));
        tokens(normalized).forEach(token -> routeRepository.searchForAdminAssistant(token).forEach(route -> routes.put(route.getId(), route)));
        return List.copyOf(routes.values());
    }

    private Stream<String> tokens(String text) {
        return Stream.of(text.split("\\s+|,|-|\\u2013|\\u2014"))
                .map(String::trim)
                .filter(token -> token.length() >= 2);
    }

    private Optional<TripOption> tripOption(Trip trip, Station origin, Station destination) {
        TripStop originStop = stop(trip, origin.getId());
        TripStop destinationStop = stop(trip, destination.getId());
        if (originStop == null || destinationStop == null
                || originStop.getDepartureOffsetMinutes() >= destinationStop.getArrivalOffsetMinutes()) {
            return Optional.empty();
        }

        Instant departure = tripInstant(trip, originStop.getDepartureOffsetMinutes());
        Instant arrival = tripInstant(trip, destinationStop.getArrivalOffsetMinutes());
        if (!departure.isAfter(Instant.now())) {
            return Optional.empty();
        }

        SeatAvailability availability = seatAvailability(trip, originStop, destinationStop);
        return Optional.of(new TripOption(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                origin.getName(),
                departure,
                passengerTime(departure, origin),
                destination.getName(),
                arrival,
                passengerTime(arrival, destination),
                trip.getRoute().getBasePrice(),
                trip.getRoute().getCurrency().getCode(),
                availability.totalSeatCount(),
                availability.availableSeatCount()
        ));
    }

    private SeatAvailability seatAvailability(Trip trip, TripStop originStop, TripStop destinationStop) {
        int totalSeats = trip.getTripCars().stream()
                .map(TripCar::getTripSeats)
                .filter(Objects::nonNull)
                .mapToInt(List::size)
                .sum();
        Set<Long> occupied = Set.copyOf(ticketRepository.findOccupiedTripSeatIdsForSegment(
                trip.getId(),
                originStop.getDepartureOffsetMinutes(),
                destinationStop.getArrivalOffsetMinutes(),
                Order.Status.PAID,
                Order.Status.PENDING,
                Instant.now(),
                Ticket.SEAT_BLOCKING_STATUSES
        ));
        return new SeatAvailability(totalSeats, Math.max(0, totalSeats - occupied.size()));
    }

    private List<TripStop> laterStops(Trip trip, TripStop originStop) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getArrivalOffsetMinutes() > originStop.getDepartureOffsetMinutes())
                .sorted(Comparator.comparing(TripStop::getArrivalOffsetMinutes))
                .toList();
    }

    private TripStop stop(Trip trip, Long stationId) {
        return trip.getTripStops().stream()
                .filter(stop -> Objects.equals(stop.getRouteStop().getStation().getId(), stationId))
                .findFirst()
                .orElse(null);
    }

    private Instant tripInstant(Trip trip, Integer offsetMinutes) {
        return trip.getDepartureAtUtc().plus(offsetMinutes, ChronoUnit.MINUTES);
    }

    private String passengerTime(Instant instant, Station station) {
        return instant.atZone(ZoneId.systemDefault()).format(PASSENGER_TIME_FORMAT);
    }

    private Instant parseDate(String value, Station station, Instant fallback) {
        if (!StringUtils.hasText(value)) {
            return fallback;
        }
        try {
            return Instant.parse(value.trim());
        } catch (DateTimeParseException ignored) {
            try {
                return LocalDate.parse(value.trim()).atStartOfDay(ZoneId.of(station.getTimezone())).toInstant();
            } catch (DateTimeParseException ignoredAgain) {
                return fallback;
            }
        }
    }

    private StationContext stationContext(Station station) {
        return new StationContext(
                station.getId(),
                station.getName(),
                station.getCountry().getCode(),
                station.getTimezone(),
                station.getAccessible()
        );
    }

    private <T> T inTx(Supplier<T> action) {
        return support.inTransaction(transactionTemplate, action);
    }

    private int limit(Integer value) {
        return support.limit(value, DEFAULT_LIMIT, MAX_LIMIT);
    }

    private record StationPair(
            Station origin,
            Station destination,
            String problem,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static StationPair ok(Station origin, Station destination) {
            return new StationPair(origin, destination, null, List.of(), List.of());
        }

        private static StationPair problem(
                String problem,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new StationPair(null, null, problem, originCandidates, destinationCandidates);
        }
    }

    private record SeatAvailability(int totalSeatCount, int availableSeatCount) {
    }

    public record TripSearchResult(
            StationContext origin,
            StationContext destination,
            String dateFromUtc,
            String dateToUtc,
            String note,
            List<TripOption> directTrips,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static TripSearchResult problem(
                String note,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new TripSearchResult(null, null, null, null, note, List.of(), originCandidates, destinationCandidates);
        }
    }

    public record TransferSearchResult(
            StationContext origin,
            StationContext destination,
            String dateFromUtc,
            String dateToUtc,
            String note,
            List<TransferOption> transferOptions,
            List<StationContext> originCandidates,
            List<StationContext> destinationCandidates
    ) {
        private static TransferSearchResult problem(
                String note,
                List<StationContext> originCandidates,
                List<StationContext> destinationCandidates
        ) {
            return new TransferSearchResult(null, null, null, null, note, List.of(), originCandidates, destinationCandidates);
        }
    }

    public record TripOption(
            Long tripId,
            Long routeId,
            String routeNumber,
            String routeName,
            String trainNumber,
            String trainName,
            String originStationName,
            Instant departureAtUtc,
            String departureAtLocal,
            String destinationStationName,
            Instant arrivalAtUtc,
            String arrivalAtLocal,
            BigDecimal basePrice,
            String currencyCode,
            int totalSeatCount,
            int availableSeatCount
    ) {
    }

    public record TransferOption(String transferStationName, long waitMinutes, TripOption firstTrip, TripOption secondTrip) {
    }

    public record PassengerReferenceData(
            List<BenefitInfo> benefits,
            List<PricedTypeInfo> trainTypes,
            List<PricedTypeInfo> carTypes,
            List<PricedTypeInfo> seatTypes,
            List<TrainInfo> trains
    ) {
    }

    public record BenefitInfo(String code, BigDecimal percentAmount) {
    }

    public record PricedTypeInfo(String code, BigDecimal priceMultiplier, String description) {
    }

    public record TrainInfo(Long trainId, String number, String name, String typeCode) {
    }

    public record TripCarInfo(Integer ordinal, String carNumber, String typeCode, Boolean accessible, int seatCount) {
    }
}
