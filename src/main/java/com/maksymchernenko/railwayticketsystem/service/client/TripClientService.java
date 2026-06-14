package com.maksymchernenko.railwayticketsystem.service.client;

import com.maksymchernenko.railwayticketsystem.dto.client.TripAvailableSeatsResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;

import java.util.List;

public interface TripClientService {

    List<TripSearchResponse> searchTrips(TripSearchRequest request);

    TripResponse getTripDetails(Long tripId);

    TripAvailableSeatsResponse getAvailableSeats(Long tripId, Long startTripStopId, Long endTripStopId);
}
