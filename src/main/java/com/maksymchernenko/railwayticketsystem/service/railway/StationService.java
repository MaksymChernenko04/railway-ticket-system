package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.reference.CodeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface StationService {

    Page<StationResponse> getAllStations(Pageable pageable);

    List<CodeResponse> getCountries();

    List<CodeResponse> getTimezones();

    StationResponse createStation(StationCreateRequest request);

    void deleteStation(Long stationId);
}
