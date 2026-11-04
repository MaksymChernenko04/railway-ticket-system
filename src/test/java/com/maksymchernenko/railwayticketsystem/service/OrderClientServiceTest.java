package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientOrderResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPaymentResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.CompletePaymentRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderSeatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.StartPaymentRequest;
import com.maksymchernenko.railwayticketsystem.exception.client.TripClientValidationException;
import com.maksymchernenko.railwayticketsystem.model.auth.User;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Passenger;
import com.maksymchernenko.railwayticketsystem.model.booking.Payment;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.railway.Train;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.reference.Benefit;
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
import com.maksymchernenko.railwayticketsystem.repository.auth.UserRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.OrderRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.PassengerRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.PaymentRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import com.maksymchernenko.railwayticketsystem.service.client.OrderClientServiceImpl;
import com.maksymchernenko.railwayticketsystem.service.client.OrderLifecycleService;
import com.maksymchernenko.railwayticketsystem.service.client.TicketLifecycleService;
import com.maksymchernenko.railwayticketsystem.service.client.TicketResponseMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.MockedStatic;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderClientServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long ORDER_ID = 900L;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private TicketRepository ticketRepository;

    @Mock
    private PassengerRepository passengerRepository;

    @Mock
    private TripRepository tripRepository;

    @Mock
    private UserRepository userRepository;

    @Spy
    private TicketResponseMapper clientTicketMapper = new TicketResponseMapper();

    @Mock
    private TicketLifecycleService ticketLifecycleService;

    @Mock
    private OrderLifecycleService orderLifecycleService;

    @InjectMocks
    private OrderClientServiceImpl orderClientService;

    @Test
    void createOrderShouldReserveSeatCalculateTotalAndReturnOrder() {
        Trip trip = createTrip();
        User user = createUser(USER_ID);
        Passenger passenger = createPassenger(700L, user, benefit("STUDENT", "50.00"));
        AtomicReference<List<Ticket>> savedTickets = new AtomicReference<>(List.of());

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(ticketRepository.findOccupiedTripSeatIdsForSegment(
                eq(1L),
                eq(0),
                eq(120),
                eq(Order.Status.PAID),
                eq(Order.Status.PENDING),
                any(Instant.class),
                eq(Ticket.SEAT_BLOCKING_STATUSES)
        )).thenReturn(List.of());
        when(passengerRepository.findByIdAndUserId(700L, USER_ID)).thenReturn(Optional.of(passenger));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(ORDER_ID);
            return order;
        });
        when(ticketRepository.saveAll(any())).thenAnswer(invocation -> {
            Iterable<Ticket> savedEntities = invocation.getArgument(0);
            List<Ticket> tickets = new ArrayList<>();
            savedEntities.forEach(tickets::add);
            for (int index = 0; index < tickets.size(); index++) {
                tickets.get(index).setId(1000L + index);
            }
            savedTickets.set(tickets);
            return tickets;
        });
        when(ticketRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenAnswer(invocation -> savedTickets.get());
        when(paymentRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenReturn(List.of());

        ClientOrderResponse result = orderClientService.createOrder(
                USER_ID,
                new CreateOrderRequest(1L, 400L, 401L, List.of(new CreateOrderSeatRequest(600L, 700L)))
        );

        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository).save(orderCaptor.capture());
        Order orderToSave = orderCaptor.getValue();
        assertThat(orderToSave.getUser()).isSameAs(user);
        assertThat(orderToSave.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(orderToSave.getExpiresAtUtc()).isAfter(orderToSave.getCreatedAtUtc());
        assertThat(orderToSave.getCurrency().getCode()).isEqualTo("UAH");

        assertThat(savedTickets.get()).hasSize(1);
        Ticket ticket = savedTickets.get().getFirst();
        assertThat(ticket.getOrder()).isSameAs(orderToSave);
        assertThat(ticket.getPassenger()).isSameAs(passenger);
        assertThat(ticket.getStartTripStop().getId()).isEqualTo(400L);
        assertThat(ticket.getEndTripStop().getId()).isEqualTo(401L);
        assertThat(ticket.getTripSeat().getId()).isEqualTo(600L);
        assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.RESERVED);
        assertThat(ticket.getAppliedBenefitCode()).isEqualTo("STUDENT");
        assertThat(ticket.getPrice()).isEqualByComparingTo("100.00");

        assertThat(result.id()).isEqualTo(ORDER_ID);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.totalAmount()).isEqualByComparingTo("100.00");
        assertThat(result.currencyCode()).isEqualTo("UAH");
        assertThat(result.tickets()).hasSize(1);
        assertThat(result.payments()).isEmpty();
    }

    @Test
    void createOrderShouldRejectOccupiedSeat() {
        Trip trip = createTrip();
        User user = createUser(USER_ID);

        when(tripRepository.findById(1L)).thenReturn(Optional.of(trip));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));
        when(ticketRepository.findOccupiedTripSeatIdsForSegment(
                eq(1L),
                eq(0),
                eq(120),
                eq(Order.Status.PAID),
                eq(Order.Status.PENDING),
                any(Instant.class),
                eq(Ticket.SEAT_BLOCKING_STATUSES)
        )).thenReturn(List.of(600L));

        assertThatThrownBy(() -> orderClientService.createOrder(
                USER_ID,
                new CreateOrderRequest(1L, 400L, 401L, List.of(new CreateOrderSeatRequest(600L, 700L)))
        ))
                .isInstanceOf(TripClientValidationException.class)
                .hasMessage("Seat is no longer available: 1A");

        verify(orderRepository, never()).save(any());
        verify(ticketRepository, never()).saveAll(any());
    }

    @Test
    void startPaymentShouldCreateProcessingPaymentForPayableOrder() {
        Order order = createPendingOrder();

        when(orderRepository.findByIdAndUserId(ORDER_ID, USER_ID)).thenReturn(Optional.of(order));
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> {
            Payment payment = invocation.getArgument(0);
            payment.setId(300L);
            return payment;
        });

        ClientPaymentResponse result = orderClientService.startPayment(
                USER_ID,
                ORDER_ID,
                new StartPaymentRequest(" card ")
        );

        verify(orderLifecycleService).expireIfOverdue(order);
        ArgumentCaptor<Payment> paymentCaptor = ArgumentCaptor.forClass(Payment.class);
        verify(paymentRepository).save(paymentCaptor.capture());
        Payment paymentToSave = paymentCaptor.getValue();
        assertThat(paymentToSave.getOrder()).isSameAs(order);
        assertThat(paymentToSave.getStatus()).isEqualTo(Payment.Status.PROCESSING);
        assertThat(paymentToSave.getMethod()).isEqualTo("card");
        assertThat(paymentToSave.getAmount()).isEqualByComparingTo("200.00");
        assertThat(paymentToSave.getCompletionTimeUtc()).isNull();

        assertThat(result.id()).isEqualTo(300L);
        assertThat(result.status()).isEqualTo("PROCESSING");
        assertThat(result.method()).isEqualTo("card");
    }

    @Test
    void completePaymentShouldMarkKnownFailingCardAsFailedAndKeepOrderPending() {
        Order order = createPendingOrder();
        Payment payment = new Payment();
        payment.setId(300L);
        payment.setOrder(order);
        payment.setStatus(Payment.Status.PROCESSING);
        payment.setMethod("card");
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());

        when(paymentRepository.findByIdAndOrderUserId(300L, USER_ID)).thenReturn(Optional.of(payment));
        when(ticketRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenReturn(List.of());
        when(paymentRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenReturn(List.of(payment));

        ClientOrderResponse result = orderClientService.completePayment(
                USER_ID,
                300L,
                new CompletePaymentRequest("4000 0000 0000 0002")
        );

        verify(orderLifecycleService).expireIfOverdue(order);
        assertThat(payment.getStatus()).isEqualTo(Payment.Status.FAILED);
        assertThat(payment.getCompletionTimeUtc()).isNull();
        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        assertThat(result.status()).isEqualTo("PENDING");
        assertThat(result.payments()).extracting("status").containsExactly("FAILED");
    }

    @Test
    void completePaymentShouldMarkPaymentSuccessfulAndActivateReservedTickets() {
        Order order = createPendingOrder();
        Trip trip = createTrip();
        Passenger passenger = createPassenger(700L, order.getUser(), benefit("STUDENT", "50.00"));
        TripStop startStop = trip.getTripStops().getFirst();
        TripStop endStop = trip.getTripStops().getLast();
        TripSeat tripSeat = trip.getTripCars().getFirst().getTripSeats().getFirst();
        Payment payment = new Payment();
        payment.setId(300L);
        payment.setOrder(order);
        payment.setStatus(Payment.Status.PROCESSING);
        payment.setMethod("card");
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        Ticket ticket = new Ticket();
        ticket.setId(1000L);
        ticket.setOrder(order);
        ticket.setPassenger(passenger);
        ticket.setStartTripStop(startStop);
        ticket.setEndTripStop(endStop);
        ticket.setTripSeat(tripSeat);
        ticket.setStatus(Ticket.Status.RESERVED);
        ticket.setCurrency(order.getCurrency());
        ticket.setAppliedBenefitCode("STUDENT");
        ticket.setAppliedBenefitPercent(new BigDecimal("50.00"));
        ticket.setPrice(new BigDecimal("100.00"));

        ThreadLocalRandom random = mock(ThreadLocalRandom.class);
        try (MockedStatic<ThreadLocalRandom> mockedRandom = mockStatic(ThreadLocalRandom.class)) {
            mockedRandom.when(ThreadLocalRandom::current).thenReturn(random);
            when(random.nextInt(100)).thenReturn(99);

            when(paymentRepository.findByIdAndOrderUserId(300L, USER_ID)).thenReturn(Optional.of(payment));
            when(ticketRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenReturn(List.of(ticket));
            when(paymentRepository.findByOrderIdOrderByIdAsc(ORDER_ID)).thenReturn(List.of(payment));

            ClientOrderResponse result = orderClientService.completePayment(
                    USER_ID,
                    300L,
                    new CompletePaymentRequest("4242 4242 4242 4242")
            );

            verify(orderLifecycleService).expireIfOverdue(order);
            assertThat(payment.getStatus()).isEqualTo(Payment.Status.SUCCESSFUL);
            assertThat(payment.getCompletionTimeUtc()).isNotNull();
            assertThat(order.getStatus()).isEqualTo(Order.Status.PAID);
            assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.ACTIVE);
            assertThat(result.status()).isEqualTo("PAID");
        }
    }

    private static Order createPendingOrder() {
        User user = createUser(USER_ID);
        Currency currency = currency();
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setUser(user);
        order.setCreatedAtUtc(Instant.now());
        order.setExpiresAtUtc(Instant.now().plusSeconds(900));
        order.setStatus(Order.Status.PENDING);
        order.setTotalAmount(new BigDecimal("200.00"));
        order.setCurrency(currency);
        return order;
    }

    private static Trip createTrip() {
        Route route = route();
        Train train = train();
        Station kyiv = station(10L, "Kyiv");
        Station lviv = station(11L, "Lviv");
        RouteStop firstRouteStop = routeStop(100L, route, kyiv, 1);
        RouteStop secondRouteStop = routeStop(101L, route, lviv, 2);

        Trip trip = new Trip();
        trip.setId(1L);
        trip.setRoute(route);
        trip.setTrain(train);
        trip.setDepartureAtUtc(Instant.now().plusSeconds(3_600));
        trip.setTripStops(List.of(
                tripStop(400L, trip, firstRouteStop, 0, 0),
                tripStop(401L, trip, secondRouteStop, 120, 125)
        ));

        Car car = car();
        TripSeat tripSeat = tripSeat(600L, seat(500L, car, "1A"));
        TripCar tripCar = new TripCar();
        tripCar.setId(550L);
        tripCar.setTrip(trip);
        tripCar.setCar(car);
        tripCar.setOrdinal(1);
        tripSeat.setTripCar(tripCar);
        tripCar.setTripSeats(List.of(tripSeat));
        trip.setTripCars(List.of(tripCar));
        return trip;
    }

    private static User createUser(Long id) {
        User user = new User();
        user.setId(id);
        user.setEmail("client@example.com");
        return user;
    }

    private static Passenger createPassenger(Long id, User user, Benefit benefit) {
        Passenger passenger = new Passenger();
        passenger.setId(id);
        passenger.setUser(user);
        passenger.setFirstName("Ivan");
        passenger.setLastName("Petrenko");
        passenger.setBenefit(benefit);
        return passenger;
    }

    private static Benefit benefit(String code, String percent) {
        Benefit benefit = new Benefit();
        benefit.setCode(code);
        benefit.setPercentAmount(new BigDecimal(percent));
        return benefit;
    }

    private static Route route() {
        Route route = new Route();
        route.setId(2L);
        route.setNumber("R1");
        route.setName("Kyiv - Lviv");
        route.setBasePrice(new BigDecimal("200.00"));
        route.setCurrency(currency());
        return route;
    }

    private static Currency currency() {
        Currency currency = new Currency();
        currency.setCode("UAH");
        return currency;
    }

    private static Train train() {
        TrainType type = new TrainType();
        type.setCode("INTERCITY");
        type.setPriceMultiplier(BigDecimal.ONE);
        Train train = new Train();
        train.setId(3L);
        train.setType(type);
        train.setNumber("625");
        train.setName("Intercity");
        return train;
    }

    private static Station station(Long id, String name) {
        Country country = new Country();
        country.setCode("UA");
        Station station = new Station();
        station.setId(id);
        station.setName(name);
        station.setCountry(country);
        station.setTimezone("Europe/Kyiv");
        station.setAccessible(true);
        return station;
    }

    private static RouteStop routeStop(Long id, Route route, Station station, Integer ordinal) {
        RouteStop routeStop = new RouteStop();
        routeStop.setId(id);
        routeStop.setRoute(route);
        routeStop.setStation(station);
        routeStop.setOrdinal(ordinal);
        return routeStop;
    }

    private static TripStop tripStop(Long id, Trip trip, RouteStop routeStop, Integer arrivalOffset, Integer departureOffset) {
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

    private static Car car() {
        CarType type = new CarType();
        type.setCode("SEATING");
        type.setPriceMultiplier(BigDecimal.ONE);
        Car car = new Car();
        car.setId(4L);
        car.setType(type);
        car.setNumber("C1");
        car.setAccessible(false);
        return car;
    }

    private static Seat seat(Long id, Car car, String number) {
        SeatType type = new SeatType();
        type.setCode("STANDARD");
        type.setPriceMultiplier(BigDecimal.ONE);
        Seat seat = new Seat();
        seat.setId(id);
        seat.setCar(car);
        seat.setType(type);
        seat.setNumber(number);
        seat.setRow(1);
        seat.setColumn(1);
        seat.setDirection(SeatDirection.FORWARD);
        return seat;
    }

    private static TripSeat tripSeat(Long id, Seat seat) {
        TripSeat tripSeat = new TripSeat();
        tripSeat.setId(id);
        tripSeat.setSeat(seat);
        return tripSeat;
    }
}
