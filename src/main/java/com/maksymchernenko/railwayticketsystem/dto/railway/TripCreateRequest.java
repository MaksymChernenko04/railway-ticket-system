package com.maksymchernenko.railwayticketsystem.dto.railway;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.List;

public record TripCreateRequest(

        @NotNull(message = "Train is required")
        Long trainId,

        @NotNull(message = "Route is required")
        Long routeId,

        @NotNull(message = "Departure time is required")
        Instant departureAtUtc,

        @NotEmpty(message = "At least one trip stop is required")
        List<@Valid TripStopCreateRequest> stops,

        @NotEmpty(message = "At least one trip car is required")
        List<@Valid TripCarCreateRequest> cars
) {}
