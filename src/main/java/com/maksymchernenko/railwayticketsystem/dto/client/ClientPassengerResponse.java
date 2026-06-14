package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;

public record ClientPassengerResponse(
        Long id,
        String firstName,
        String lastName,
        String benefitCode,
        BigDecimal benefitPercent
) {
}
