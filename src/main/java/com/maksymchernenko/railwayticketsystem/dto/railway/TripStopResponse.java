package com.maksymchernenko.railwayticketsystem.dto.railway;

import java.time.Instant;

public record TripStopResponse(
        Long id,
        Long routeStopId,
        Long stationId,
        String stationName,
        String stationCountryCode,
        Integer ordinal,
        Integer platform,
        Integer track,
        Integer arrivalOffsetMinutes,
        Integer departureOffsetMinutes,
        Instant arrivalAtUtc,
        Instant departureAtUtc
) {}
