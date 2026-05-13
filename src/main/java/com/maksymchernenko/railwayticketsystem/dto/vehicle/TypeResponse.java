package com.maksymchernenko.railwayticketsystem.dto.vehicle;

import java.math.BigDecimal;

public record TypeResponse(

        String code,
        BigDecimal priceMultiplier
) {}
