package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.model.booking.Ticket;
import com.maksymchernenko.railwayticketsystem.repository.booking.OrderRepository;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private final OrderRepository orderRepository;
    private final TicketRepository ticketRepository;

    @Override
    @Transactional
    public void expireOverduePendingOrdersForUser(Long userId) {
        Instant now = Instant.now();
        orderRepository.findByUserIdAndStatusAndExpiresAtUtcLessThanEqual(userId, Order.Status.PENDING, now)
                .forEach(this::expire);
    }

    @Override
    @Transactional
    public void expireIfOverdue(Order order) {
        if (order.getStatus() == Order.Status.PENDING && !order.getExpiresAtUtc().isAfter(Instant.now())) {
            expire(order);
        }
    }

    private void expire(Order order) {
        order.setStatus(Order.Status.EXPIRED);
        ticketRepository.findByOrderIdOrderByIdAsc(order.getId())
                .forEach(ticket -> ticket.setStatus(Ticket.Status.CANCELLED));
    }
}
