package com.maksymchernenko.railwayticketsystem.dto.railway;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;

public record RouteCreateRequest(

        @NotBlank(message = "Number is required")
        @Size(max = 20, message = "Number must be less than 20 characters")
        String number,

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be less than 255 characters")
        String name,

        @NotNull(message = "Base price is required")
        @Digits(integer = 10, fraction = 2)
        @DecimalMin(value = "0", message = "Price must be >= 0")
        BigDecimal basePrice,

        @NotBlank(message = "Currency code is required")
        @Size(min = 3, max = 3, message = "Currency code must be exactly 3 characters")
        String currencyCode
) {}
