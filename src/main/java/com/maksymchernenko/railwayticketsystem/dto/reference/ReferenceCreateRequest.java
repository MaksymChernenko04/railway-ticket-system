package com.maksymchernenko.railwayticketsystem.dto.reference;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ReferenceCreateRequest(

        @NotBlank(message = "Code is required")
        @Size(max = 50, message = "Code must be less than 50 characters")
        String code,

        BigDecimal priceMultiplier,

        BigDecimal percentAmount
) {}
