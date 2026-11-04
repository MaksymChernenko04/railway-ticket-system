package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.TripAvailableSeatsResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripCarSeatAvailabilityResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSeatAvailabilityResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import com.maksymchernenko.railwayticketsystem.exception.client.TripClientValidationException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.service.railway.TripService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TripClientServiceImpl implements TripClientService {

    private final TripRepository tripRepository;
    private final StationRepository stationRepository;
    private final TicketRepository ticketRepository;
    private final TripService tripService;

    @Transactional(readOnly = true)
    public List<TripSearchResponse> searchTrips(TripSearchRequest request) {
        if (Objects.equals(request.originStationId(), request.destinationStationId())) {
            throw new TripClientValidationException("Origin and destination stations must be different");
        }

        Station originStation = stationRepository.findById(request.originStationId())
                .orElseThrow(() -> new EntityNotFoundException("Origin station not found: " + request.originStationId()));
        stationRepository.findById(request.destinationStationId())
                .orElseThrow(() -> new EntityNotFoundException("Destination station not found: " + request.destinationStationId()));

        ZoneId originZone = ZoneId.of(originStation.getTimezone());
        Instant departureFromUtc = request.travelDate().atStartOfDay(originZone).toInstant();
        Instant departureToUtc = request.travelDate().plusDays(1).atStartOfDay(originZone).toInstant();
        Instant nowUtc = Instant.now();

        return tripRepository.findTripsContainingSegmentDepartingBetween(
                        request.originStationId(),
                        request.destinationStationId(),
                        departureFromUtc,
                        departureToUtc,
                        nowUtc
                )
                .stream()
                .map(trip -> toSearchResponse(trip, originZone, request))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(TripSearchResponse::departureAtUtc))
                .toList();
    }

    @Transactional(readOnly = true)
    public TripResponse getTripDetails(Long tripId) {
        return tripService.getTrip(tripId);
    }

    @Transactional(readOnly = true)
    public TripAvailableSeatsResponse getAvailableSeats(Long tripId, Long startTripStopId, Long endTripStopId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + tripId));

        TripStop startStop = findTripStop(trip, startTripStopId, "Start trip stop not found: ");
        TripStop endStop = findTripStop(trip, endTripStopId, "End trip stop not found: ");

        Integer startDepartureOffset = startStop.getDepartureOffsetMinutes();
        Integer endArrivalOffset = endStop.getArrivalOffsetMinutes();
        if (startDepartureOffset >= endArrivalOffset) {
            throw new TripClientValidationException("Start trip stop must be before end trip stop");
        }

        Set<Long> occupiedTripSeatIds = Set.copyOf(ticketRepository.findOccupiedTripSeatIdsForSegment(
                tripId,
                startDepartureOffset,
                endArrivalOffset,
                Order.Status.PAID,
                Order.Status.PENDING,
                Instant.now(),
                Ticket.SEAT_BLOCKING_STATUSES
        ));

        List<TripCarSeatAvailabilityResponse> cars = trip.getTripCars().stream()
                .sorted(Comparator.comparing(TripCar::getOrdinal))
                .map(tripCar -> toCarSeatAvailabilityResponse(tripCar, occupiedTripSeatIds))
                .toList();

        return new TripAvailableSeatsResponse(tripId, startTripStopId, endTripStopId, cars);
    }

    private TripSearchResponse toSearchResponse(
            Trip trip,
            ZoneId originZone,
            TripSearchRequest request
    ) {
        TripStop originTripStop = findTripStopByStationId(trip, request.originStationId());
        TripStop destinationTripStop = findTripStopByStationId(trip, request.destinationStationId());
        if (originTripStop == null || destinationTripStop == null) {
            return null;
        }

        if (originTripStop.getDepartureOffsetMinutes() >= destinationTripStop.getArrivalOffsetMinutes()) {
            return null;
        }

        var departureAtUtc = trip.getDepartureAtUtc().plus(originTripStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);
        if (!departureAtUtc.isAfter(Instant.now())) {
            return null;
        }

        if (!departureAtUtc.atZone(originZone).toLocalDate().equals(request.travelDate())) {
            return null;
        }

        var arrivalAtUtc = trip.getDepartureAtUtc().plus(destinationTripStop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES);

        return new TripSearchResponse(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getId(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                originTripStop.getId(),
                originTripStop.getRouteStop().getStation().getId(),
                originTripStop.getRouteStop().getStation().getName(),
                departureAtUtc,
                destinationTripStop.getId(),
                destinationTripStop.getRouteStop().getStation().getId(),
                destinationTripStop.getRouteStop().getStation().getName(),
                arrivalAtUtc,
                trip.getRoute().getBasePrice(),
                trip.getRoute().getCurrency().getCode()
        );
    }

    private TripStop findTripStop(Trip trip, Long tripStopId, String messagePrefix) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getId().equals(tripStopId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messagePrefix + tripStopId));
    }

    private TripStop findTripStopByStationId(Trip trip, Long stationId) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getRouteStop().getStation().getId().equals(stationId))
                .findFirst()
                .orElse(null);
    }

    private TripCarSeatAvailabilityResponse toCarSeatAvailabilityResponse(
            TripCar tripCar,
            Set<Long> occupiedTripSeatIds
    ) {
        Car car = tripCar.getCar();
        List<TripSeatAvailabilityResponse> seats = tripCar.getTripSeats().stream()
                .sorted(Comparator.comparing(tripSeat -> tripSeat.getSeat().getNumber()))
                .map(tripSeat -> toSeatAvailabilityResponse(tripSeat, occupiedTripSeatIds))
                .toList();

        return new TripCarSeatAvailabilityResponse(
                tripCar.getId(),
                car.getId(),
                car.getNumber(),
                car.getType().getCode(),
                car.getAccessible(),
                tripCar.getOrdinal(),
                seats
        );
    }

    private TripSeatAvailabilityResponse toSeatAvailabilityResponse(
            TripSeat tripSeat,
            Set<Long> occupiedTripSeatIds
    ) {
        Seat seat = tripSeat.getSeat();

        return new TripSeatAvailabilityResponse(
                tripSeat.getId(),
                seat.getId(),
                seat.getNumber(),
                seat.getType().getCode(),
                seat.getRow(),
                seat.getColumn(),
                seat.getDirection(),
                !occupiedTripSeatIds.contains(tripSeat.getId())
        );
    }
}
