package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;
import java.time.Instant;

public record ClientPaymentResponse(
        Long id,
        Long orderId,
        String status,
        String method,
        BigDecimal amount,
        String currencyCode,
        Instant completionTimeUtc
) {
}
