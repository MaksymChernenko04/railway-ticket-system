package com.maksymchernenko.railwayticketsystem.dto.reference;

import java.math.BigDecimal;

public record ReferenceResponse(

        String code,
        BigDecimal priceMultiplier,
        BigDecimal percentAmount
) {}
