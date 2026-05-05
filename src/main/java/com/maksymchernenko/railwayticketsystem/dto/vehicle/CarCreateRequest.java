package com.maksymchernenko.railwayticketsystem.dto.vehicle;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CarCreateRequest(

        @NotBlank(message = "Type code is required")
        @Size(max = 30, message = "Type code must be less than 30 characters")
        String typeCode,

        @NotBlank(message = "Number is required")
        @Size(max = 20, message = "Number must be less than 20 characters")
        String number,

        @NotNull(message = "Accessible flag is required")
        Boolean accessible
) {}
