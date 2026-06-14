package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.reference.CodeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.StationResponse;
import com.maksymchernenko.railwayticketsystem.service.railway.StationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/stations")
@RequiredArgsConstructor
public class StationAdminController {

    private final StationService stationService;

    @GetMapping
    public Page<StationResponse> getStations(Pageable pageable) {
        return stationService.getAllStations(pageable);
    }

    @GetMapping("/countries")
    public List<CodeResponse> getCountries() {
        return stationService.getCountries();
    }

    @GetMapping("/timezones")
    public List<CodeResponse> getTimezones() {
        return stationService.getTimezones();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StationResponse createStation(@Valid @RequestBody StationCreateRequest request) {
        return stationService.createStation(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteStation(@PathVariable Long id) {
        stationService.deleteStation(id);
    }
}
