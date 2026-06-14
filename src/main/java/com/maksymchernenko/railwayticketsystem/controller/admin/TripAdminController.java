package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.railway.TripCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import com.maksymchernenko.railwayticketsystem.service.railway.TripService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/trips")
@RequiredArgsConstructor
public class TripAdminController {

    private final TripService tripService;

    @GetMapping
    public Page<TripResponse> getTrips(Pageable pageable) {
        return tripService.getTrips(pageable);
    }

    @GetMapping("/{id}")
    public TripResponse getTrip(@PathVariable Long id) {
        return tripService.getTrip(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TripResponse createTrip(@Valid @RequestBody TripCreateRequest request) {
        return tripService.createTrip(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTrip(@PathVariable Long id) {
        tripService.deleteTrip(id);
    }
}
