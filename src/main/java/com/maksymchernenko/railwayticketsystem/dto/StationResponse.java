package com.maksymchernenko.railwayticketsystem.dto;

public record StationResponse(
        Long id,
        String name,
        String countryCode,
        String timezone,
        String address,
        Boolean accessible
) {}
