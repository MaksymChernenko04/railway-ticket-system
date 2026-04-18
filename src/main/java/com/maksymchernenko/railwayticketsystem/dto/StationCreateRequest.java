package com.maksymchernenko.railwayticketsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record StationCreateRequest(

        @NotBlank(message = "Name is required")
        @Size(max = 255, message = "Name must be less than 255 characters")
        String name,

        @NotBlank(message = "Country code is required")
        @Size(min = 2, max = 2, message = "Country code must be 2 characters")
        String countryCode,

        @NotBlank(message = "Timezone is required")
        @Size(max = 50, message = "Timezone must be less than 50 characters")
        String timezone,

        @Size(max = 255, message = "Address must be less than 255 characters")
        String address,

        @NotNull(message = "Accessible flag is required")
        Boolean accessible
) {}
