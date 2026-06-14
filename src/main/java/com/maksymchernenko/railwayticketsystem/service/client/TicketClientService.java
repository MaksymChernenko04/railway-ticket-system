package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientTicketResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TicketClientService {

    Page<ClientTicketResponse> getTickets(Long userId, Pageable pageable);
}
