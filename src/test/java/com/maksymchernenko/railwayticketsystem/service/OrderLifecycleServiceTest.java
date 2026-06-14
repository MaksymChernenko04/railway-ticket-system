package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.repository.booking.OrderRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import com.maksymchernenko.railwayticketsystem.service.client.OrderLifecycleServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderLifecycleServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private TicketRepository ticketRepository;

    @InjectMocks
    private OrderLifecycleServiceImpl orderLifecycleService;

    @Test
    void expireOverduePendingOrdersForUserShouldExpireOrderAndCancelReservedTickets() {
        Order order = createOrder(10L, Instant.now().minusSeconds(60));
        Ticket ticket = createTicket(Ticket.Status.RESERVED);

        when(orderRepository.findByUserIdAndStatusAndExpiresAtUtcLessThanEqual(
                any(Long.class),
                any(Order.Status.class),
                any(Instant.class)
        )).thenReturn(List.of(order));
        when(ticketRepository.findByOrderIdOrderByIdAsc(10L)).thenReturn(List.of(ticket));

        orderLifecycleService.expireOverduePendingOrdersForUser(1L);

        assertThat(order.getStatus()).isEqualTo(Order.Status.EXPIRED);
        assertThat(ticket.getStatus()).isEqualTo(Ticket.Status.CANCELLED);
        verify(orderRepository).findByUserIdAndStatusAndExpiresAtUtcLessThanEqual(
                any(Long.class),
                any(Order.Status.class),
                any(Instant.class)
        );
    }

    @Test
    void expireIfOverdueShouldLeaveActiveReservationUnchanged() {
        Order order = createOrder(10L, Instant.now().plusSeconds(60));

        orderLifecycleService.expireIfOverdue(order);

        assertThat(order.getStatus()).isEqualTo(Order.Status.PENDING);
        verify(ticketRepository, never()).findByOrderIdOrderByIdAsc(10L);
    }

    private Order createOrder(Long id, Instant expiresAtUtc) {
        Order order = new Order();
        order.setId(id);
        order.setStatus(Order.Status.PENDING);
        order.setExpiresAtUtc(expiresAtUtc);
        return order;
    }

    private Ticket createTicket(Ticket.Status status) {
        Ticket ticket = new Ticket();
        ticket.setStatus(status);
        return ticket;
    }
}
