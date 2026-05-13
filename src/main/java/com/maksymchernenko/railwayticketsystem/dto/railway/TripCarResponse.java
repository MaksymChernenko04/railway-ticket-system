package com.maksymchernenko.railwayticketsystem.dto.railway;

import java.util.List;

public record TripCarResponse(
        Long id,
        Long carId,
        String carNumber,
        String carTypeCode,
        Boolean accessible,
        Integer ordinal,
        List<TripSeatResponse> seats
) {}
