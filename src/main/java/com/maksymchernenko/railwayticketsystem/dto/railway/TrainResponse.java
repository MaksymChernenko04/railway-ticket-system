package com.maksymchernenko.railwayticketsystem.dto.railway;

public record TrainResponse(

        Long id,
        String typeCode,
        String number,
        String name
) {}
