package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;

public record ClientBenefitResponse(
        String code,
        BigDecimal percentAmount
) {
}
