package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;

public record ClientPassengerCreateRequest(
        @NotBlank(message = "First name is required")
        @Size(max = 100, message = "First name must not exceed 100 characters")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Size(max = 100, message = "Last name must not exceed 100 characters")
        String lastName,

        String benefitCode
) {
}
