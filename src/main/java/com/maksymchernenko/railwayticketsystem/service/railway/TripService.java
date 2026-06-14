package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.railway.TripCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface TripService {

    Page<TripResponse> getTrips(Pageable pageable);

    TripResponse getTrip(Long id);

    TripResponse createTrip(TripCreateRequest request);

    void deleteTrip(Long id);
}
