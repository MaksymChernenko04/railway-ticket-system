package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientOrderResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientPaymentResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.ClientTicketResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.CompletePaymentRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.CreateOrderSeatRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.StartPaymentRequest;
import com.maksymchernenko.railwayticketsystem.exception.client.TripClientValidationException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.auth.User;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Passenger;
import com.maksymchernenko.railwayticketsystem.model.booking.Payment;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.model.reference.Benefit;
import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import com.maksymchernenko.railwayticketsystem.repository.auth.UserRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.OrderRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.PassengerRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.PaymentRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.TripRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderClientServiceImpl implements OrderClientService {

    private static final int PAYMENT_WINDOW_MINUTES = 15;

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final TicketRepository ticketRepository;
    private final PassengerRepository passengerRepository;
    private final TripRepository tripRepository;
    private final UserRepository userRepository;
    private final TicketResponseMapper clientTicketMapper;
    private final TicketLifecycleService ticketLifecycleService;
    private final OrderLifecycleService orderLifecycleService;

    @Override
    @Transactional
    public ClientOrderResponse createOrder(Long userId, CreateOrderRequest request) {
        Instant now = Instant.now();
        Trip trip = tripRepository.findById(request.tripId())
                .orElseThrow(() -> new EntityNotFoundException("Trip not found: " + request.tripId()));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new EntityNotFoundException("User not found: " + userId));

        TripStop startStop = findTripStop(trip, request.startTripStopId(), "Departure stop not found: ");
        TripStop endStop = findTripStop(trip, request.endTripStopId(), "Arrival stop not found: ");
        validateTripSegment(trip, startStop, endStop, now);

        List<CreateOrderSeatRequest> requestedSeats = request.seats();
        validateUniqueSelections(requestedSeats);

        Map<Integer, TripSeat> tripSeatsById = trip.getTripCars().stream()
                .flatMap(tripCar -> tripCar.getTripSeats().stream())
                .collect(Collectors.toMap(TripSeat::getId, Function.identity()));
        List<TripSeat> selectedSeats = requestedSeats.stream()
                .map(selection -> {
                    TripSeat tripSeat = tripSeatsById.get(selection.tripSeatId());
                    if (tripSeat == null) {
                        throw new EntityNotFoundException("Seat not found on this trip: " + selection.tripSeatId());
                    }
                    return tripSeat;
                })
                .toList();

        Set<Integer> occupiedTripSeatIds = Set.copyOf(ticketRepository.findOccupiedTripSeatIdsForSegment(
                trip.getId(),
                startStop.getDepartureOffsetMinutes(),
                endStop.getArrivalOffsetMinutes(),
                Order.Status.PAID,
                Order.Status.PENDING,
                now,
                Ticket.SEAT_BLOCKING_STATUSES
        ));
        selectedSeats.stream()
                .filter(seat -> occupiedTripSeatIds.contains(seat.getId()))
                .findFirst()
                .ifPresent(seat -> {
                    throw new TripClientValidationException("Seat is no longer available: " + seat.getSeat().getNumber());
                });

        Map<Long, Passenger> passengersById = loadPassengers(userId, requestedSeats);

        Order order = new Order();
        order.setUser(user);
        order.setCreatedAtUtc(now);
        order.setExpiresAtUtc(now.plus(PAYMENT_WINDOW_MINUTES, ChronoUnit.MINUTES));
        order.setStatus(Order.Status.PENDING);
        order.setCurrency(trip.getRoute().getCurrency());
        order.setTotalAmount(BigDecimal.ZERO);
        Order savedOrder = orderRepository.save(order);

        Map<Integer, TripSeat> selectedSeatById = selectedSeats.stream()
                .collect(Collectors.toMap(TripSeat::getId, Function.identity()));
        List<Ticket> tickets = requestedSeats.stream()
                .map(selection -> createTicket(
                        savedOrder,
                        passengersById.get(selection.passengerId()),
                        startStop,
                        endStop,
                        selectedSeatById.get(selection.tripSeatId()),
                        trip
                ))
                .toList();
        ticketRepository.saveAll(tickets);

        BigDecimal total = tickets.stream()
                .map(Ticket::getPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);
        savedOrder.setTotalAmount(total);

        return toResponse(savedOrder);
    }

    @Override
    @Transactional
    public ClientOrderResponse getOrder(Long userId, Long orderId) {
        orderLifecycleService.expireOverduePendingOrdersForUser(userId);
        ticketLifecycleService.finishCompletedActiveTicketsForUser(userId);
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));
        return toResponse(order);
    }

    @Override
    @Transactional
    public Page<ClientOrderResponse> getOrders(Long userId, Pageable pageable) {
        orderLifecycleService.expireOverduePendingOrdersForUser(userId);
        ticketLifecycleService.finishCompletedActiveTicketsForUser(userId);
        return orderRepository.findByUserIdOrderByCreatedAtUtcDescIdDesc(userId, pageable)
                .map(this::toResponse);
    }

    @Override
    @Transactional
    public ClientPaymentResponse startPayment(Long userId, Long orderId, StartPaymentRequest request) {
        Instant now = Instant.now();
        Order order = orderRepository.findByIdAndUserId(orderId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Order not found: " + orderId));

        orderLifecycleService.expireIfOverdue(order);
        ensureOrderCanBePaid(order, now);

        Payment payment = new Payment();
        payment.setOrder(order);
        payment.setMethod(request.method().trim());
        payment.setAmount(order.getTotalAmount());
        payment.setCurrency(order.getCurrency());
        payment.setCompletionTimeUtc(null);
        payment.setStatus(Payment.Status.PROCESSING);

        return toPaymentResponse(paymentRepository.save(payment));
    }

    @Override
    @Transactional(readOnly = true)
    public ClientPaymentResponse getPayment(Long userId, Long paymentId) {
        Payment payment = paymentRepository.findByIdAndOrderUserId(paymentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        return toPaymentResponse(payment);
    }

    @Override
    @Transactional
    public ClientOrderResponse completePayment(Long userId, Long paymentId, CompletePaymentRequest request) {
        Instant now = Instant.now();
        Payment payment = paymentRepository.findByIdAndOrderUserId(paymentId, userId)
                .orElseThrow(() -> new EntityNotFoundException("Payment not found: " + paymentId));
        Order order = payment.getOrder();

        orderLifecycleService.expireIfOverdue(order);
        if (payment.getStatus() != Payment.Status.PROCESSING) {
            throw new TripClientValidationException("Payment is already completed");
        }

        ensureOrderCanBePaid(order, now);

        boolean successful = isPaymentSuccessful(request);
        payment.setStatus(successful ? Payment.Status.SUCCESSFUL : Payment.Status.FAILED);
        payment.setCompletionTimeUtc(successful ? now : null);

        if (successful) {
            order.setStatus(Order.Status.PAID);
            ticketRepository.findByOrderIdOrderByIdAsc(order.getId())
                    .forEach(ticket -> ticket.setStatus(Ticket.Status.ACTIVE));
        }

        return toResponse(order);
    }

    private void ensureOrderCanBePaid(Order order, Instant now) {
        if (order.getStatus() == Order.Status.PAID) {
            throw new TripClientValidationException("Order is already paid");
        }
        if (order.getStatus() == Order.Status.CANCELLED || order.getStatus() == Order.Status.EXPIRED) {
            throw new TripClientValidationException("Order can no longer be paid");
        }
        if (!order.getExpiresAtUtc().isAfter(now)) {
            orderLifecycleService.expireIfOverdue(order);
            throw new TripClientValidationException("Order payment time has expired");
        }
    }

    private boolean isPaymentSuccessful(CompletePaymentRequest request) {
        String normalizedCardNumber = request.cardNumber() != null
                ? request.cardNumber().replaceAll("\\D", "")
                : "";
        if ("4000000000000002".equals(normalizedCardNumber)) {
            return false;
        }

        return ThreadLocalRandom.current().nextInt(100) >= 5;
    }

    private void validateTripSegment(Trip trip, TripStop startStop, TripStop endStop, Instant now) {
        if (startStop.getDepartureOffsetMinutes() >= endStop.getArrivalOffsetMinutes()) {
            throw new TripClientValidationException("Departure stop must be before arrival stop");
        }

        Instant departureAtUtc = trip.getDepartureAtUtc().plus(startStop.getDepartureOffsetMinutes(), ChronoUnit.MINUTES);
        if (!departureAtUtc.isAfter(now)) {
            throw new TripClientValidationException("Cannot create an order for a departed trip");
        }
    }

    private void validateUniqueSelections(List<CreateOrderSeatRequest> seats) {
        Set<Integer> seatIds = new HashSet<>();
        Set<Long> passengerIds = new HashSet<>();
        for (CreateOrderSeatRequest seat : seats) {
            if (!seatIds.add(seat.tripSeatId())) {
                throw new TripClientValidationException("Each selected seat must be unique");
            }
            if (!passengerIds.add(seat.passengerId())) {
                throw new TripClientValidationException("Each passenger can be assigned to only one seat");
            }
        }
    }

    private Map<Long, Passenger> loadPassengers(Long userId, List<CreateOrderSeatRequest> seats) {
        List<Long> passengerIds = seats.stream()
                .map(CreateOrderSeatRequest::passengerId)
                .distinct()
                .toList();

        Map<Long, Passenger> passengersById = passengerIds.stream()
                .map(passengerId -> passengerRepository.findByIdAndUserId(passengerId, userId)
                        .orElseThrow(() -> new EntityNotFoundException("Passenger not found: " + passengerId)))
                .collect(Collectors.toMap(Passenger::getId, Function.identity(), (left, right) -> left, LinkedHashMap::new));

        if (passengersById.size() != passengerIds.size()) {
            throw new TripClientValidationException("Passenger selection is invalid");
        }
        return passengersById;
    }

    private Ticket createTicket(
            Order order,
            Passenger passenger,
            TripStop startStop,
            TripStop endStop,
            TripSeat tripSeat,
            Trip trip
    ) {
        Benefit benefit = passenger.getBenefit();

        Ticket ticket = new Ticket();
        ticket.setOrder(order);
        ticket.setPassenger(passenger);
        ticket.setStartTripStop(startStop);
        ticket.setEndTripStop(endStop);
        ticket.setTripSeat(tripSeat);
        ticket.setStatus(Ticket.Status.RESERVED);
        ticket.setCurrency(trip.getRoute().getCurrency());
        ticket.setAppliedBenefitCode(benefit != null ? benefit.getCode() : null);
        ticket.setAppliedBenefitPercent(benefit != null ? benefit.getPercentAmount() : BigDecimal.ZERO);
        ticket.setPrice(calculatePrice(trip, startStop, endStop, tripSeat, benefit));
        return ticket;
    }

    private BigDecimal calculatePrice(Trip trip, TripStop startStop, TripStop endStop, TripSeat tripSeat, Benefit benefit) {
        int stopCount = Math.max(2, trip.getTripStops().size());
        int segmentLegs = Math.max(1, endStop.getRouteStop().getOrdinal() - startStop.getRouteStop().getOrdinal());

        BigDecimal routeShare = BigDecimal.valueOf(segmentLegs)
                .divide(BigDecimal.valueOf(stopCount - 1L), 4, RoundingMode.HALF_UP);
        BigDecimal benefitPercent = benefit != null ? benefit.getPercentAmount() : BigDecimal.ZERO;
        BigDecimal discountMultiplier = BigDecimal.ONE.subtract(
                benefitPercent.divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
        );

        return trip.getRoute().getBasePrice()
                .multiply(routeShare)
                .multiply(trip.getTrain().getType().getPriceMultiplier())
                .multiply(tripSeat.getTripCar().getCar().getType().getPriceMultiplier())
                .multiply(tripSeat.getSeat().getType().getPriceMultiplier())
                .multiply(discountMultiplier)
                .setScale(2, RoundingMode.HALF_UP);
    }

    private TripStop findTripStop(Trip trip, Long tripStopId, String messagePrefix) {
        return trip.getTripStops().stream()
                .filter(stop -> stop.getId().equals(tripStopId))
                .findFirst()
                .orElseThrow(() -> new EntityNotFoundException(messagePrefix + tripStopId));
    }

    private ClientOrderResponse toResponse(Order order) {
        List<ClientTicketResponse> tickets = ticketRepository.findByOrderIdOrderByIdAsc(order.getId())
                .stream()
                .sorted(Comparator.comparing(Ticket::getId))
                .map(clientTicketMapper::toResponse)
                .toList();
        List<ClientPaymentResponse> payments = paymentRepository.findByOrderIdOrderByIdAsc(order.getId())
                .stream()
                .map(this::toPaymentResponse)
                .toList();

        return new ClientOrderResponse(
                order.getId(),
                order.getStatus().name(),
                order.getCreatedAtUtc(),
                order.getExpiresAtUtc(),
                order.getTotalAmount(),
                order.getCurrency().getCode(),
                tickets,
                payments
        );
    }

    private ClientPaymentResponse toPaymentResponse(Payment payment) {
        return new ClientPaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getStatus().name(),
                payment.getMethod(),
                payment.getAmount(),
                payment.getCurrency().getCode(),
                payment.getCompletionTimeUtc()
        );
    }
}
