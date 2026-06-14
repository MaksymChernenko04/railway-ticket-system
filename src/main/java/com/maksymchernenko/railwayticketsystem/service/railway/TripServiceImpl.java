package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.railway.TripCarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripCarResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripSeatResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripStopResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TrainRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.CarRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class TripServiceImpl implements TripService {

    private final TripRepository tripRepository;
    private final TrainRepository trainRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;
    private final CarRepository carRepository;
    private final SeatRepository seatRepository;

    @Transactional(readOnly = true)
    public Page<TripResponse> getTrips(Pageable pageable) {
        return tripRepository.findAll(pageable)
                .map(this::toSummaryResponse);
    }

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long id) {
        Trip trip = tripRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + id));

        return toResponse(trip);
    }

    @Transactional
    public TripResponse createTrip(TripCreateRequest request) {
        Train train = trainRepository.findById(request.trainId())
                .orElseThrow(() -> new EntityNotFoundException("Train not found: " + request.trainId()));

        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + request.routeId()));

        Trip trip = new Trip();
        trip.setTrain(train);
        trip.setRoute(route);
        trip.setDepartureAtUtc(request.departureAtUtc());
        trip.setTripStops(buildTripStops(trip, route, request.stops()));
        trip.setTripCars(buildTripCars(trip, request.cars()));

        return toResponse(tripRepository.save(trip));
    }

    @Transactional
    public void deleteTrip(Long id) {
        if (!tripRepository.existsById(id)) {
            throw new EntityNotFoundException("Trip not found: " + id);
        }

        tripRepository.deleteById(id);
    }

    private List<TripStop> buildTripStops(Trip trip, Route route, List<TripStopCreateRequest> requests) {
        Set<Long> routeStopIds = new HashSet<>();
        List<TripStop> stops = new ArrayList<>();
        int previousDepartureOffset = -1;
        int previousRouteOrdinal = 0;

        for (TripStopCreateRequest request : requests) {
            if (!routeStopIds.add(request.routeStopId())) {
                throw new EntityAlreadyExistsException("Route stop is selected more than once: " + request.routeStopId());
            }

            if (request.arrivalOffsetMinutes() > request.departureOffsetMinutes()) {
                throw new EntityAlreadyExistsException("Arrival offset cannot be greater than departure offset");
            }

            if (request.arrivalOffsetMinutes() < previousDepartureOffset) {
                throw new EntityAlreadyExistsException("Stop offsets must follow the route order");
            }

            RouteStop routeStop = routeStopRepository.findById(request.routeStopId())
                    .orElseThrow(() -> new EntityNotFoundException("Route stop not found: " + request.routeStopId()));

            if (!routeStop.getRoute().getId().equals(route.getId())) {
                throw new EntityAlreadyExistsException("Route stop does not belong to selected route: " + request.routeStopId());
            }

            if (routeStop.getOrdinal() <= previousRouteOrdinal) {
                throw new EntityAlreadyExistsException("Trip stops must follow the route stop order");
            }

            TripStop tripStop = new TripStop();
            tripStop.setTrip(trip);
            tripStop.setRouteStop(routeStop);
            tripStop.setPlatform(request.platform());
            tripStop.setTrack(request.track());
            tripStop.setArrivalOffsetMinutes(request.arrivalOffsetMinutes());
            tripStop.setDepartureOffsetMinutes(request.departureOffsetMinutes());
            stops.add(tripStop);
            previousDepartureOffset = request.departureOffsetMinutes();
            previousRouteOrdinal = routeStop.getOrdinal();
        }

        return stops;
    }

    private List<TripCar> buildTripCars(Trip trip, List<TripCarCreateRequest> requests) {
        Set<Long> carIds = new HashSet<>();
        Set<Integer> ordinals = new HashSet<>();
        List<TripCar> tripCars = new ArrayList<>();

        for (TripCarCreateRequest request : requests) {
            if (!carIds.add(request.carId())) {
                throw new EntityAlreadyExistsException("Car is selected more than once: " + request.carId());
            }

            if (!ordinals.add(request.ordinal())) {
                throw new EntityAlreadyExistsException("Car ordinal is selected more than once: " + request.ordinal());
            }

            Car car = carRepository.findById(request.carId())
                    .orElseThrow(() -> new EntityNotFoundException("Car not found: " + request.carId()));

            TripCar tripCar = new TripCar();
            tripCar.setTrip(trip);
            tripCar.setCar(car);
            tripCar.setOrdinal(request.ordinal());
            tripCar.setTripSeats(buildTripSeats(tripCar, car, request.seatIds()));
            tripCars.add(tripCar);
        }

        return tripCars;
    }

    private List<TripSeat> buildTripSeats(TripCar tripCar, Car car, List<Long> seatIds) {
        Set<Long> selectedSeatIds = new HashSet<>();
        List<TripSeat> tripSeats = new ArrayList<>();

        for (Long seatId : seatIds) {
            if (!selectedSeatIds.add(seatId)) {
                throw new EntityAlreadyExistsException("Seat is selected more than once: " + seatId);
            }

            Seat seat = seatRepository.findById(seatId)
                    .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + seatId));

            if (!seat.getCar().getId().equals(car.getId())) {
                throw new EntityAlreadyExistsException("Seat does not belong to selected car: " + seatId);
            }

            TripSeat tripSeat = new TripSeat();
            tripSeat.setTripCar(tripCar);
            tripSeat.setSeat(seat);
            tripSeats.add(tripSeat);
        }

        return tripSeats;
    }

    private TripResponse toSummaryResponse(Trip trip) {
        return new TripResponse(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getId(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                trip.getTrain().getType().getCode(),
                trip.getDepartureAtUtc(),
                trip.getTripStops() == null ? 0 : trip.getTripStops().size(),
                trip.getTripCars() == null ? 0 : trip.getTripCars().size(),
                countSeats(trip.getTripCars()),
                List.of(),
                List.of()
        );
    }

    private TripResponse toResponse(Trip trip) {
        List<TripStopResponse> stops = trip.getTripStops().stream()
                .sorted(Comparator.comparing(stop -> stop.getRouteStop().getOrdinal()))
                .map(stop -> toStopResponse(trip, stop))
                .toList();

        List<TripCarResponse> cars = trip.getTripCars().stream()
                .sorted(Comparator.comparing(TripCar::getOrdinal))
                .map(this::toCarResponse)
                .toList();

        return new TripResponse(
                trip.getId(),
                trip.getRoute().getId(),
                trip.getRoute().getNumber(),
                trip.getRoute().getName(),
                trip.getTrain().getId(),
                trip.getTrain().getNumber(),
                trip.getTrain().getName(),
                trip.getTrain().getType().getCode(),
                trip.getDepartureAtUtc(),
                stops.size(),
                cars.size(),
                countSeats(trip.getTripCars()),
                stops,
                cars
        );
    }

    private TripStopResponse toStopResponse(Trip trip, TripStop stop) {
        RouteStop routeStop = stop.getRouteStop();

        return new TripStopResponse(
                stop.getId(),
                routeStop.getId(),
                routeStop.getStation().getId(),
                routeStop.getStation().getName(),
                routeStop.getStation().getCountry().getCode(),
                routeStop.getOrdinal(),
                stop.getPlatform(),
                stop.getTrack(),
                stop.getArrivalOffsetMinutes(),
                stop.getDepartureOffsetMinutes(),
                trip.getDepartureAtUtc().plus(stop.getArrivalOffsetMinutes(), ChronoUnit.MINUTES),
                trip.getDepartureAtUtc().plus(stop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES)
        );
    }

    private TripCarResponse toCarResponse(TripCar tripCar) {
        Car car = tripCar.getCar();
        List<TripSeatResponse> seats = tripCar.getTripSeats().stream()
                .sorted(Comparator.comparing(seat -> seat.getSeat().getNumber()))
                .map(this::toSeatResponse)
                .toList();

        return new TripCarResponse(
                tripCar.getId(),
                car.getId(),
                car.getNumber(),
                car.getType().getCode(),
                car.getAccessible(),
                tripCar.getOrdinal(),
                seats
        );
    }

    private TripSeatResponse toSeatResponse(TripSeat tripSeat) {
        Seat seat = tripSeat.getSeat();

        return new TripSeatResponse(
                tripSeat.getId(),
                seat.getId(),
                seat.getNumber(),
                seat.getType().getCode(),
                seat.getRow(),
                seat.getColumn(),
                seat.getDirection()
        );
    }

    private int countSeats(List<TripCar> tripCars) {
        if (tripCars == null) {
            return 0;
        }

        return tripCars.stream()
                .mapToInt(tripCar -> tripCar.getTripSeats() == null ? 0 : tripCar.getTripSeats().size())
                .sum();
    }
}
