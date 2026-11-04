package com.maksymchernenko.railwayticketsystem.dto.client;

import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;

public record TripSeatAvailabilityResponse(
        Long tripSeatId,
        Long seatId,
        String number,
        String seatTypeCode,
        Integer row,
        Integer column,
        SeatDirection direction,
        Boolean available
) {}
