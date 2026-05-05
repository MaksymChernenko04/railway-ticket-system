package com.maksymchernenko.railwayticketsystem.dto.railway;

public record RouteStopResponse(
        Long id,
        Long routeId,
        String routeNumber,
        Long stationId,
        String stationName,
        String stationCountryCode,
        Integer ordinal
) {}
