package com.maksymchernenko.railwayticketsystem.dto.client;

import java.util.List;

public record TripAvailableSeatsResponse(
        Long tripId,
        Long startTripStopId,
        Long endTripStopId,
        List<TripCarSeatAvailabilityResponse> cars
) {}
