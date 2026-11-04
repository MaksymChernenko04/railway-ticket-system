package com.maksymchernenko.railwayticketsystem.dto.ai.context;

public record RouteStopContext(
        Long stationId,
        String stationName,
        String countryCode,
        String timezone,
        Integer ordinal
) {
}
