package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.client.TripAvailableSeatsResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchResponse;
import com.maksymchernenko.railwayticketsystem.exception.client.TripClientValidationException;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.reference.CarType;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.service.client.TripClientServiceImpl;
import com.maksymchernenko.railwayticketsystem.service.railway.TripService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TripClientServiceTest {

    private static final Instant TRIP_DEPARTURE = Instant.parse("2035-06-02T22:00:00Z");

    @Mock
    private TripRepository tripRepository;

    @Mock
    private StationRepository stationRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private TripService tripService;

    @InjectMocks
    private TripClientServiceImpl tripClientService;

    @Test
    void searchTripsShouldReturnTripsForRequestedOriginLocalDate() {
        Station kyiv = createStation(10L, "Kyiv", "Europe/Kyiv");
        Station lviv = createStation(11L, "Lviv", "Europe/Kyiv");
        Route route = createRoute();
        RouteStop originStop = createRouteStop(100L, route, kyiv, 1);
        RouteStop destinationStop = createRouteStop(101L, route, lviv, 2);
        Trip matchingTrip = createTrip(route, originStop, destinationStop);

        when(stationRepository.findById(10L)).thenReturn(Optional.of(kyiv));
        when(stationRepository.findById(11L)).thenReturn(Optional.of(lviv));
        when(tripRepository.findTripsContainingSegment(10L, 11L)).thenReturn(List.of(matchingTrip));

        List<TripSearchResponse> result = tripClientService.searchTrips(new TripSearchRequest(
                10L,
                11L,
                LocalDate.parse("2035-06-03")
        ));

        assertThat(result).hasSize(1);
        TripSearchResponse response = result.getFirst();
        assertThat(response.tripId()).isEqualTo(1L);
        assertThat(response.routeNumber()).isEqualTo("R1");
        assertThat(response.trainNumber()).isEqualTo("625");
        assertThat(response.departureTripStopId()).isEqualTo(400L);
        assertThat(response.arrivalTripStopId()).isEqualTo(401L);
        assertThat(response.departureAtUtc()).isEqualTo(Instant.parse("2035-06-03T01:00:00Z"));
        assertThat(response.arrivalAtUtc()).isEqualTo(Instant.parse("2035-06-03T04:00:00Z"));
        assertThat(response.basePrice()).isEqualByComparingTo("350.00");
        assertThat(response.currencyCode()).isEqualTo("UAH");
    }

    @Test
    void searchTripsShouldExcludePastDepartures() {
        Station kyiv = createStation(10L, "Kyiv", "Europe/Kyiv");
        Station lviv = createStation(11L, "Lviv", "Europe/Kyiv");
        Route route = createRoute();
        RouteStop originStop = createRouteStop(100L, route, kyiv, 1);
        RouteStop destinationStop = createRouteStop(101L, route, lviv, 2);
        Trip pastTrip = createTrip(route, originStop, destinationStop);
        pastTrip.setDepartureAtUtc(Instant.parse("2020-06-02T22:00:00Z"));

        when(stationRepository.findById(10L)).thenReturn(Optional.of(kyiv));
        when(stationRepository.findById(11L)).thenReturn(Optional.of(lviv));
        when(tripRepository.findTripsContainingSegment(10L, 11L)).thenReturn(List.of(pastTrip));

        List<TripSearchResponse> result = tripClientService.searchTrips(new TripSearchRequest(
                10L,
                11L,
                LocalDate.parse("2020-06-03")
        ));

        assertThat(result).isEmpty();
    }

    @Test
    void searchTripsShouldRejectSameOriginAndDestination() {
        assertThatThrownBy(() -> tripClientService.searchTrips(new TripSearchRequest(
                10L,
                10L,
                LocalDate.parse("2026-06-03")
        )))
                .isInstanceOf(TripClientValidationException.class)
                .hasMessage("Origin and destination stations must be different");

        verify(tripRepository, never()).findTripsContainingSegment(10L, 10L);
    }

    @Test
    void getAvailableSeatsShouldMarkOverlappingTicketsAsUnavailable() {
        Route route = createRoute();
        Station kyiv = createStation(10L, "Kyiv", "Europe/Kyiv");
        Station lviv = createStation(11L, "Lviv", "Europe/Kyiv");
        RouteStop originStop = createRouteStop(100L, route, kyiv, 1);
        RouteStop destinationStop = createRouteStop(101L, route, lviv, 2);
        Trip trip = createTrip(route, originStop, destinationStop);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(ticketRepository.findOccupiedTripSeatIdsForSegment(
                eq(1L),
                eq(180),
                eq(360),
                eq(Order.Status.PAID),
                eq(Order.Status.PENDING),
                org.mockito.ArgumentMatchers.any(Instant.class),
                eq(Ticket.SEAT_BLOCKING_STATUSES)
        )).thenReturn(List.of(601));

        TripAvailableSeatsResponse result = tripClientService.getAvailableSeats(1L, 400L, 401L);

        assertThat(result.tripId()).isEqualTo(1L);
        assertThat(result.cars()).hasSize(1);
        assertThat(result.cars().getFirst().seats()).hasSize(2);
        assertThat(result.cars().getFirst().seats().get(0).tripSeatId()).isEqualTo(600);
        assertThat(result.cars().getFirst().seats().get(0).available()).isTrue();
        assertThat(result.cars().getFirst().seats().get(1).tripSeatId()).isEqualTo(601);
        assertThat(result.cars().getFirst().seats().get(1).available()).isFalse();
    }

    @Test
    void getAvailableSeatsShouldRejectReverseSegment() {
        Route route = createRoute();
        Station kyiv = createStation(10L, "Kyiv", "Europe/Kyiv");
        Station lviv = createStation(11L, "Lviv", "Europe/Kyiv");
        Trip trip = createTrip(
                route,
                createRouteStop(100L, route, kyiv, 1),
                createRouteStop(101L, route, lviv, 2)
        );

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));

        assertThatThrownBy(() -> tripClientService.getAvailableSeats(1L, 401L, 400L))
                .isInstanceOf(TripClientValidationException.class)
                .hasMessage("Start trip stop must be before end trip stop");

        verify(ticketRepository, never()).findOccupiedTripSeatIdsForSegment(
                eq(1L),
                eq(365),
                eq(180),
                eq(Order.Status.PAID),
                eq(Order.Status.PENDING),
                org.mockito.ArgumentMatchers.any(Instant.class),
                org.mockito.ArgumentMatchers.<Collection<Ticket.Status>>any()
        );
    }

    private Trip createTrip(Route route, RouteStop originStop, RouteStop destinationStop) {
        Car car = createCar();
        Trip trip = new Trip();
        trip.setId(1L);
        trip.setRoute(route);
        trip.setTrain(createTrain());
        trip.setDepartureAtUtc(TRIP_DEPARTURE);
        trip.setTripStops(List.of(
                createTripStop(400L, trip, originStop, 180, 180),
                createTripStop(401L, trip, destinationStop, 360, 365)
        ));
        trip.setTripCars(List.of(createTripCar(500L, trip, car, List.of(
                createTripSeat(600, createSeat(300L, car, "1A")),
                createTripSeat(601, createSeat(301L, car, "1B"))
        ))));
        return trip;
    }

    private Route createRoute() {
        Currency currency = new Currency();
        currency.setCode("UAH");

        Route route = new Route();
        route.setId(2L);
        route.setNumber("R1");
        route.setName("Kyiv - Lviv");
        route.setBasePrice(new BigDecimal("350.00"));
        route.setCurrency(currency);
        return route;
    }

    private Train createTrain() {
        TrainType type = new TrainType();
        type.setCode("INTERCITY");

        Train train = new Train();
        train.setId(3L);
        train.setType(type);
        train.setNumber("625");
        train.setName("Anubis");
        return train;
    }

    private Station createStation(Long id, String name, String timezone) {
        Country country = new Country();
        country.setCode("UA");

        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCountry(country);
        station.setTimezone(timezone);
        station.setAccessible(true);
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

    private Car createCar() {
        CarType carType = new CarType();
        carType.setCode("COUPE_SLEEPER");

        Car car = new Car();
        car.setId(200L);
        car.setType(carType);
        car.setNumber("C1");
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

    private TripCar createTripCar(Long id, Trip trip, Car car, List<TripSeat> seats) {
        TripCar tripCar = new TripCar();
        tripCar.setId(id);
        tripCar.setTrip(trip);
        tripCar.setCar(car);
        tripCar.setOrdinal(1);
        seats.forEach(seat -> seat.setTripCar(tripCar));
        tripCar.setTripSeats(seats);
        return tripCar;
    }
}
