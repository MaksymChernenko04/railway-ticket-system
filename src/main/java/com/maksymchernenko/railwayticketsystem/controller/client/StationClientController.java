package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.railway.StationResponse;
import com.maksymchernenko.railwayticketsystem.service.railway.StationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/client/stations")
@RequiredArgsConstructor
public class StationClientController {

    private final StationService stationService;

    @GetMapping
    public Page<StationResponse> getStations(Pageable pageable) {
        return stationService.getAllStations(pageable);
    }
}
