package com.maksymchernenko.railwayticketsystem.dto.railway;

import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;

public record TripSeatResponse(
        Integer id,
        Long seatId,
        String number,
        String typeCode,
        Integer row,
        Integer column,
        SeatDirection direction
) {}
