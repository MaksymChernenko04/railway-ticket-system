package com.maksymchernenko.railwayticketsystem.dto.railway;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record RouteStopCreateRequest(

        @NotNull(message = "Route is required")
        Long routeId,

        @NotNull(message = "Station is required")
        Long stationId,

        @NotNull(message = "Ordinal is required")
        @Min(value = 1, message = "Ordinal must be greater than 0")
        Integer ordinal
) {}
