package com.maksymchernenko.railwayticketsystem.dto.railway;

public record StationResponse(
        Long id,
        String name,
        String countryCode,
        String timezone,
        String address,
        Boolean accessible
) {}
