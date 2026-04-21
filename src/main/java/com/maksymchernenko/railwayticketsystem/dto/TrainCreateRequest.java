package com.maksymchernenko.railwayticketsystem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TrainCreateRequest(

        @NotBlank(message = "Type code is required")
        @Size(max = 20, message = "Type code must be less than 30 characters")
        String typeCode,

        @NotBlank(message = "Number is required")
        @Size(max = 50, message = "Number must be less than 50 characters")
        String number,

        @Size(max = 100, message = "Name must be less than 100 characters")
        String name
) {}
