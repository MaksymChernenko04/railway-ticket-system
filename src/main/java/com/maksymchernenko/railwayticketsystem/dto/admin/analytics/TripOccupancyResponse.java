package com.maksymchernenko.railwayticketsystem.dto.admin.analytics;

import java.time.Instant;

public record TripOccupancyResponse(
        Long tripId,
        Long routeId,
        String routeNumber,
        String routeName,
        Instant departureAtUtc,
        long carCount,
        long totalSeatCount,
        long soldSeatCount,
        double occupancyPercent
) {
}
