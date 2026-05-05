package com.maksymchernenko.railwayticketsystem.dto.vehicle;

public record CarResponse(

        Long id,
        String typeCode,
        String number,
        Boolean accessible
) {}
