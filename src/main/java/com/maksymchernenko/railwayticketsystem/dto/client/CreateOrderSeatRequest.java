package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.constraints.NotNull;

public record CreateOrderSeatRequest(
        @NotNull(message = "Seat is required")
        Integer tripSeatId,

        @NotNull(message = "Passenger is required")
        Long passengerId
) {
}
