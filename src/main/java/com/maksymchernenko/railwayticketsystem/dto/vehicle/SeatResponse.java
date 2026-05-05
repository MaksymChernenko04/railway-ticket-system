package com.maksymchernenko.railwayticketsystem.dto.vehicle;

public record SeatResponse(

        Long id,
        String typeCode,
        String number,
        Integer row,
        Integer column,
        com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection direction
) {}
