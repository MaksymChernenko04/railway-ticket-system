package com.maksymchernenko.railwayticketsystem.dto.client;

import java.util.List;

public record TripCarSeatAvailabilityResponse(
        Long tripCarId,
        Long carId,
        String carNumber,
        String carTypeCode,
        Boolean accessible,
        Integer ordinal,
        List<TripSeatAvailabilityResponse> seats
) {}
