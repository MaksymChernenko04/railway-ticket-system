package com.maksymchernenko.railwayticketsystem.dto.railway;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record TripStopCreateRequest(

        @NotNull(message = "Route stop is required")
        Long routeStopId,

        @NotNull(message = "Platform is required")
        @Min(value = 1, message = "Platform must be greater than 0")
        Integer platform,

        @NotNull(message = "Track is required")
        @Min(value = 1, message = "Track must be greater than 0")
        Integer track,

        @NotNull(message = "Arrival offset is required")
        @Min(value = 0, message = "Arrival offset cannot be negative")
        Integer arrivalOffsetMinutes,

        @NotNull(message = "Departure offset is required")
        @Min(value = 0, message = "Departure offset cannot be negative")
        Integer departureOffsetMinutes
) {}
