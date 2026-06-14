package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.TripCarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.reference.CarType;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
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
import com.maksymchernenko.railwayticketsystem.service.railway.TripServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripServiceTest {

    private static final Instant DEPARTURE = Instant.parse("2026-05-10T08:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private TrainRepository trainRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private CarRepository carRepository;

    @Mock
    private SeatRepository seatRepository;

    @InjectMocks
    private TripServiceImpl tripService;

    @Test
    void getTripsShouldMapEntitiesToSummaryResponses() {
        Pageable pageable = PageRequest.of(0, 2);
        Trip trip = createTrip();

        when(tripRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(trip), pageable, 1));

        Page<TripResponse> result = tripService.getTrips(pageable);

        TripResponse response = result.getContent().getFirst();
        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.routeNumber()).isEqualTo("R1");
        assertThat(response.trainNumber()).isEqualTo("625");
        assertThat(response.departureAtUtc()).isEqualTo(DEPARTURE);
        assertThat(response.stopCount()).isEqualTo(2);
        assertThat(response.carCount()).isEqualTo(1);
        assertThat(response.seatCount()).isEqualTo(2);
        assertThat(response.stops()).isEmpty();
        assertThat(response.cars()).isEmpty();
    }

    @Test
    void getTripShouldMapFullTripDetailsOrderedByRouteStopAndCarOrdinal() {
        Trip trip = createTripWithUnorderedChildren();

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        TripResponse result = tripService.getTrip(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.stops()).extracting("stationName").containsExactly("Kyiv", "Lviv");
        assertThat(result.stops()).extracting("arrivalAtUtc").containsExactly(
                DEPARTURE,
                DEPARTURE.plusSeconds(7_200)
        );
        assertThat(result.cars()).extracting("ordinal").containsExactly(1, 2);
        assertThat(result.cars().getFirst().seats()).extracting("number").containsExactly("1A", "1B");
    }

    @Test
    void createTripShouldBuildAggregateAndReturnResponse() {
        Route route = createRoute();
        Train train = createTrain();
        RouteStop kyivStop = createRouteStop(100L, route, createStation(10L, "Kyiv"), 1);
        RouteStop lvivStop = createRouteStop(101L, route, createStation(11L, "Lviv"), 2);
        Car car = createCar(200L, "C1");
        Seat firstSeat = createSeat(300L, car, "1A");
        Seat secondSeat = createSeat(301L, car, "1B");
        TripCreateRequest request = createRequest();

        when(trainRepository.findById(1L)).thenReturn(Optional.of(train));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(100L)).thenReturn(Optional.of(kyivStop));
        when(routeStopRepository.findById(101L)).thenReturn(Optional.of(lvivStop));
        when(carRepository.findById(200L)).thenReturn(Optional.of(car));
        when(seatRepository.findById(300L)).thenReturn(Optional.of(firstSeat));
        when(seatRepository.findById(301L)).thenReturn(Optional.of(secondSeat));
        when(tripRepository.save(any(Trip.class))).thenAnswer(invocation -> {
            Trip trip = invocation.getArgument(0);
            trip.setId(1L);
            trip.getTripStops().get(0).setId(400L);
            trip.getTripStops().get(1).setId(401L);
            trip.getTripCars().getFirst().setId(500L);
            trip.getTripCars().getFirst().getTripSeats().get(0).setId(600);
            trip.getTripCars().getFirst().getTripSeats().get(1).setId(601);
            return trip;
        });

        TripResponse result = tripService.createTrip(request);

        ArgumentCaptor<Trip> tripCaptor = ArgumentCaptor.forClass(Trip.class);
        verify(tripRepository).save(tripCaptor.capture());
        Trip tripToSave = tripCaptor.getValue();

        assertThat(tripToSave.getTrain()).isSameAs(train);
        assertThat(tripToSave.getRoute()).isSameAs(route);
        assertThat(tripToSave.getDepartureAtUtc()).isEqualTo(DEPARTURE);
        assertThat(tripToSave.getTripStops()).hasSize(2);
        assertThat(tripToSave.getTripStops()).extracting(TripStop::getTrip).containsOnly(tripToSave);
        assertThat(tripToSave.getTripStops()).extracting(TripStop::getRouteStop).containsExactly(kyivStop, lvivStop);
        assertThat(tripToSave.getTripCars()).hasSize(1);
        assertThat(tripToSave.getTripCars().getFirst().getTrip()).isSameAs(tripToSave);
        assertThat(tripToSave.getTripCars().getFirst().getCar()).isSameAs(car);
        assertThat(tripToSave.getTripCars().getFirst().getTripSeats()).extracting(TripSeat::getSeat)
                .containsExactly(firstSeat, secondSeat);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.stopCount()).isEqualTo(2);
        assertThat(result.carCount()).isEqualTo(1);
        assertThat(result.seatCount()).isEqualTo(2);
        assertThat(result.stops()).extracting("stationName").containsExactly("Kyiv", "Lviv");
        assertThat(result.cars().getFirst().seats()).extracting("number").containsExactly("1A", "1B");
    }

    @Test
    void createTripShouldThrowWhenRouteStopDoesNotBelongToRoute() {
        Route selectedRoute = createRoute();
        Route otherRoute = new Route();
        otherRoute.setId(99L);
        RouteStop routeStop = createRouteStop(100L, otherRoute, createStation(10L, "Kyiv"), 1);

        when(trainRepository.findById(1L)).thenReturn(Optional.of(createTrain()));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(selectedRoute));
        when(routeStopRepository.findById(100L)).thenReturn(Optional.of(routeStop));

        assertThatThrownBy(() -> tripService.createTrip(createRequest()))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Route stop does not belong to selected route: 100");

        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTripShouldThrowWhenTripStopsDoNotFollowRouteOrder() {
        Route route = createRoute();
        RouteStop kyivStop = createRouteStop(100L, route, createStation(10L, "Kyiv"), 2);
        RouteStop lvivStop = createRouteStop(101L, route, createStation(11L, "Lviv"), 1);

        when(trainRepository.findById(1L)).thenReturn(Optional.of(createTrain()));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(100L)).thenReturn(Optional.of(kyivStop));
        when(routeStopRepository.findById(101L)).thenReturn(Optional.of(lvivStop));

        assertThatThrownBy(() -> tripService.createTrip(createRequest()))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Trip stops must follow the route stop order");

        verify(tripRepository, never()).save(any());
    }

    @Test
    void createTripShouldThrowWhenSeatDoesNotBelongToSelectedCar() {
        Route route = createRoute();
        Car selectedCar = createCar(200L, "C1");
        Car otherCar = createCar(201L, "C2");

        when(trainRepository.findById(1L)).thenReturn(Optional.of(createTrain()));
        when(routeRepository.findById(2L)).thenReturn(Optional.of(route));
        when(routeStopRepository.findById(100L))
                .thenReturn(Optional.of(createRouteStop(100L, route, createStation(10L, "Kyiv"), 1)));
        when(routeStopRepository.findById(101L))
                .thenReturn(Optional.of(createRouteStop(101L, route, createStation(11L, "Lviv"), 2)));
        when(carRepository.findById(200L)).thenReturn(Optional.of(selectedCar));
        when(seatRepository.findById(300L)).thenReturn(Optional.of(createSeat(300L, selectedCar, "1A")));
        when(seatRepository.findById(301L)).thenReturn(Optional.of(createSeat(301L, otherCar, "1B")));

        assertThatThrownBy(() -> tripService.createTrip(createRequest()))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Seat does not belong to selected car: 301");

        verify(tripRepository, never()).save(any());
    }

    @Test
    void getTripShouldThrowWhenTripNotFound() {
        when(tripRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> tripService.getTrip(1L))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Trip not found: 1");
    }

    private TripCreateRequest createRequest() {
        return new TripCreateRequest(
                1L,
                2L,
                DEPARTURE,
                List.of(
                        new TripStopCreateRequest(100L, 1, 1, 0, 0),
                        new TripStopCreateRequest(101L, 2, 3, 120, 125)
                ),
                List.of(new TripCarCreateRequest(200L, 1, List.of(300L, 301L)))
        );
    }

    private Trip createTrip() {
        Route route = createRoute();
        Car car = createCar(200L, "C1");
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setRoute(route);
        trip.setTrain(createTrain());
        trip.setDepartureAtUtc(DEPARTURE);

        TripStop firstStop = createTripStop(400L, trip, createRouteStop(100L, route, createStation(10L, "Kyiv"), 1), 0, 0);
        TripStop secondStop = createTripStop(401L, trip, createRouteStop(101L, route, createStation(11L, "Lviv"), 2), 120, 125);

        TripCar tripCar = createTripCar(500L, trip, car, 1, List.of(
                createTripSeat(600, createSeat(300L, car, "1A")),
                createTripSeat(601, createSeat(301L, car, "1B"))
        ));

        trip.setTripStops(List.of(firstStop, secondStop));
        trip.setTripCars(List.of(tripCar));
        return trip;
    }

    private Trip createTripWithUnorderedChildren() {
        Trip trip = createTrip();
        Route route = trip.getRoute();
        Car secondCar = createCar(201L, "C2");
        TripCar unorderedFirstCar = createTripCar(501L, trip, secondCar, 2, List.of());
        TripCar unorderedSecondCar = trip.getTripCars().getFirst();

        trip.setTripStops(List.of(
                createTripStop(401L, trip, createRouteStop(101L, route, createStation(11L, "Lviv"), 2), 120, 125),
                createTripStop(400L, trip, createRouteStop(100L, route, createStation(10L, "Kyiv"), 1), 0, 0)
        ));
        trip.setTripCars(List.of(unorderedFirstCar, unorderedSecondCar));
        return trip;
    }

    private Route createRoute() {
        Route route = new Route();
        route.setId(2L);
        route.setNumber("R1");
        route.setName("Kyiv - Lviv");
        return route;
    }

    private Train createTrain() {
        TrainType type = new TrainType();
        type.setCode("INTERCITY");

        Train train = new Train();
        train.setId(1L);
        train.setType(type);
        train.setNumber("625");
        train.setName("Anubis");
        return train;
    }

    private Station createStation(Long id, String name) {
        Country country = new Country();
        country.setCode("UA");

        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCountry(country);
        return station;
    }

    private RouteStop createRouteStop(Long id, Route route, Station station, Integer ordinal) {
        RouteStop routeStop = new RouteStop();
        routeStop.setId(id);
        routeStop.setRoute(route);
        routeStop.setStation(station);
        routeStop.setOrdinal(ordinal);
        return routeStop;
    }

    private TripStop createTripStop(Long id, Trip trip, RouteStop routeStop, Integer arrivalOffset, Integer departureOffset) {
        TripStop tripStop = new TripStop();
        tripStop.setId(id);
        tripStop.setTrip(trip);
        tripStop.setRouteStop(routeStop);
        tripStop.setPlatform(1);
        tripStop.setTrack(1);
        tripStop.setArrivalOffsetMinutes(arrivalOffset);
        tripStop.setDepartureOffsetMinutes(departureOffset);
        return tripStop;
    }

    private Car createCar(Long id, String number) {
        CarType carType = new CarType();
        carType.setCode("COUPE_SLEEPER");

        Car car = new Car();
        car.setId(id);
        car.setType(carType);
        car.setNumber(number);
        car.setAccessible(false);
        return car;
    }

    private Seat createSeat(Long id, Car car, String number) {
        SeatType seatType = new SeatType();
        seatType.setCode("LOWER");

        Seat seat = new Seat();
        seat.setId(id);
        seat.setCar(car);
        seat.setType(seatType);
        seat.setNumber(number);
        seat.setRow(1);
        seat.setColumn(number.endsWith("A") ? 1 : 2);
        seat.setDirection(SeatDirection.FORWARD);
        return seat;
    }

    private TripSeat createTripSeat(Integer id, Seat seat) {
        TripSeat tripSeat = new TripSeat();
        tripSeat.setId(id);
        tripSeat.setSeat(seat);
        return tripSeat;
    }

    private TripCar createTripCar(Long id, Trip trip, Car car, Integer ordinal, List<TripSeat> seats) {
        TripCar tripCar = new TripCar();
        tripCar.setId(id);
        tripCar.setTrip(trip);
        tripCar.setCar(car);
        tripCar.setOrdinal(ordinal);
        seats.forEach(seat -> seat.setTripCar(tripCar));
        tripCar.setTripSeats(seats);
        return tripCar;
    }
}
