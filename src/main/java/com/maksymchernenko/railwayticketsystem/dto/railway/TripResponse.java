package com.maksymchernenko.railwayticketsystem.dto.railway;

import java.time.Instant;
import java.util.List;

public record TripResponse(
        Long id,
        Long routeId,
        String routeNumber,
        String routeName,
        Long trainId,
        String trainNumber,
        String trainName,
        String trainTypeCode,
        Instant departureAtUtc,
        Integer stopCount,
        Integer carCount,
        Integer seatCount,
        List<TripStopResponse> stops,
        List<TripCarResponse> cars
) {}
