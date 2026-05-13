package com.maksymchernenko.railwayticketsystem.dto.railway;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record TripCarCreateRequest(

        @NotNull(message = "Car is required")
        Long carId,

        @NotNull(message = "Ordinal is required")
        @Min(value = 1, message = "Ordinal must be greater than 0")
        Integer ordinal,

        @NotEmpty(message = "At least one seat is required")
        List<@NotNull(message = "Seat is required") Long> seatIds
) {}
