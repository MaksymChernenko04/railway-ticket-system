package com.maksymchernenko.railwayticketsystem.dto;

import java.math.BigDecimal;

public record RouteResponse(
        Long id,
        String number,
        String name,
        BigDecimal basePrice,
        String currencyCode
) {}
