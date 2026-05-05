package com.maksymchernenko.railwayticketsystem.dto.vehicle;

import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SeatCreateRequest(

        @NotBlank(message = "Type code is required")
        @Size(max = 30, message = "Type code must be less than 30 characters")
        String typeCode,

        @NotBlank(message = "Number is required")
        @Size(max = 10, message = "Number must be less than 10 characters")
        String number,

        @NotNull(message = "Row is required")
        @Min(value = 1, message = "Row must be at least 1")
        Integer row,

        @NotNull(message = "Column is required")
        @Min(value = 1, message = "Column must be at least 1")
        Integer column,

        @NotNull(message = "Direction is required")
        SeatDirection direction
) {}
