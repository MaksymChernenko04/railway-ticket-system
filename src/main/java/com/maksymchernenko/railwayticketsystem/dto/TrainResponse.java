package com.maksymchernenko.railwayticketsystem.dto;

public record TrainResponse(

        Long id,
        String typeCode,
        String number,
        String name
) {}
