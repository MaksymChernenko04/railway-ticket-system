package com.maksymchernenko.railwayticketsystem.dto.ai.context;

public record StationContext(
        Long stationId,
        String stationName,
        String countryCode,
        String timezone,
        Boolean accessible
) {
}
