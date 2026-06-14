package com.maksymchernenko.railwayticketsystem.dto.client;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record TripSearchRequest(
        @NotNull Long originStationId,
        @NotNull Long destinationStationId,
        @NotNull LocalDate travelDate
) {}
