package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class TicketLifecycleServiceImpl implements TicketLifecycleService {

    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public void finishCompletedActiveTicketsForUser(Long userId) {
        Instant now = Instant.now();
        ticketRepository.findTicketsByUserAndStatus(userId, Order.Status.PAID, Ticket.Status.ACTIVE).stream()
                .filter(ticket -> !arrivalAtUtc(ticket).isAfter(now))
                .forEach(ticket -> ticket.setStatus(Ticket.Status.FINISHED));
    }

    private Instant arrivalAtUtc(Ticket ticket) {
        return ticket.getEndTripStop().getTrip().getDepartureAtUtc()
                .plus(ticket.getEndTripStop().getArrivalOffsetMinutes(), ChronoUnit.MINUTES);
    }
}
