package com.maksymchernenko.railwayticketsystem.controller;

import com.maksymchernenko.railwayticketsystem.dto.StationCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.StationResponse;
import com.maksymchernenko.railwayticketsystem.service.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/stations")
@RequiredArgsConstructor
public class StationAdminController {

    private final StationService stationService;

    @GetMapping
    public Page<StationResponse> getStations(Pageable pageable) {
        return stationService.getAllStations(pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(@Valid @RequestBody StationCreateRequest request) {
        return stationService.createStation(request);
    }
}
