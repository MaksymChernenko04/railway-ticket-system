package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "Trip is required")
        Long tripId,

        @NotNull(message = "Departure stop is required")
        Long startTripStopId,

        @NotNull(message = "Arrival stop is required")
        Long endTripStopId,

        @Valid
        @NotEmpty(message = "At least one seat is required")
        @Size(max = 6, message = "One order can contain up to 6 tickets")
        List<CreateOrderSeatRequest> seats
) {
}
