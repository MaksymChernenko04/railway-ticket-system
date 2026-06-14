package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.ClientTicketResponse;
import com.maksymchernenko.railwayticketsystem.security.CustomUserDetails;
import com.maksymchernenko.railwayticketsystem.service.client.TicketClientService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/tickets")
@RequiredArgsConstructor
public class TicketClientController {

    private final TicketClientService ticketClientService;

    @GetMapping
    public Page<ClientTicketResponse> getTickets(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            Pageable pageable
    ) {
        return ticketClientService.getTickets(userDetails.getId(), pageable);
    }
}
