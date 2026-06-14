package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record ClientOrderResponse(
        Long id,
        String status,
        Instant createdAtUtc,
        Instant expiresAtUtc,
        BigDecimal totalAmount,
        String currencyCode,
        List<ClientTicketResponse> tickets,
        List<ClientPaymentResponse> payments
) {
}
