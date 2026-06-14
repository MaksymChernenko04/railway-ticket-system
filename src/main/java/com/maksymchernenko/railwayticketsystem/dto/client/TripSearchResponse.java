package com.maksymchernenko.railwayticketsystem.dto.client;

import java.math.BigDecimal;
import java.time.Instant;

public record TripSearchResponse(
        Long tripId,
        Long routeId,
        String routeNumber,
        String routeName,
        Long trainId,
        String trainNumber,
        String trainName,
        Long departureTripStopId,
        Long departureStationId,
        String departureStationName,
        Instant departureAtUtc,
        Long arrivalTripStopId,
        Long arrivalStationId,
        String arrivalStationName,
        Instant arrivalAtUtc,
        BigDecimal basePrice,
        String currencyCode
) {}
