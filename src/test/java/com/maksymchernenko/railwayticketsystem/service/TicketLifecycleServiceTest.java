package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import com.maksymchernenko.railwayticketsystem.model.railway.TripStop;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.service.client.TicketLifecycleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TicketLifecycleServiceTest {

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private TicketLifecycleServiceImpl ticketLifecycleService;

    @Test
    void finishCompletedActiveTicketsForUserShouldFinishOnlyArrivedTickets() {
        Ticket completedTicket = createActiveTicket(Instant.now().minusSeconds(3_600), 30);
        Ticket upcomingTicket = createActiveTicket(Instant.now().plusSeconds(3_600), 30);

        when(ticketRepository.findTicketsByUserAndStatus(1L, Order.Status.PAID, Ticket.Status.ACTIVE))
                .thenReturn(List.of(completedTicket, upcomingTicket));

        ticketLifecycleService.finishCompletedActiveTicketsForUser(1L);

        assertThat(completedTicket.getStatus()).isEqualTo(Ticket.Status.FINISHED);
        assertThat(upcomingTicket.getStatus()).isEqualTo(Ticket.Status.ACTIVE);
        verify(ticketRepository).findTicketsByUserAndStatus(1L, Order.Status.PAID, Ticket.Status.ACTIVE);
    }

    private Ticket createActiveTicket(Instant departureAtUtc, int arrivalOffsetMinutes) {
        Trip trip = new Trip();
        trip.setDepartureAtUtc(departureAtUtc);

        TripStop endTripStop = new TripStop();
        endTripStop.setTrip(trip);
        endTripStop.setArrivalOffsetMinutes(arrivalOffsetMinutes);

        Ticket ticket = new Ticket();
        ticket.setStatus(Ticket.Status.ACTIVE);
        ticket.setEndTripStop(endTripStop);
        return ticket;
    }
}
