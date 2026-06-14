package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientTicketResponse;
import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import com.maksymchernenko.railwayticketsystem.repository.booking.TicketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TicketClientServiceImpl implements TicketClientService {

    private final TicketRepository ticketRepository;
    private final TicketResponseMapper clientTicketMapper;
    private final TicketLifecycleService ticketLifecycleService;

    @Override
    @Transactional
    public Page<ClientTicketResponse> getTickets(Long userId, Pageable pageable) {
        ticketLifecycleService.finishCompletedActiveTicketsForUser(userId);
        return ticketRepository.findVisibleTicketsByUser(userId, Order.Status.PAID, pageable)
                .map(clientTicketMapper::toResponse);
    }
}
