package com.maksymchernenko.railwayticketsystem.controller.client;

import com.maksymchernenko.railwayticketsystem.dto.client.TripAvailableSeatsResponse;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchRequest;
import com.maksymchernenko.railwayticketsystem.dto.client.TripSearchResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.TripResponse;
import com.maksymchernenko.railwayticketsystem.service.client.TripClientService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/client/trips")
@RequiredArgsConstructor
public class TripClientController {

    private final TripClientService tripClientService;

    @GetMapping("/search")
    public List<TripSearchResponse> searchTrips(@Valid @ModelAttribute TripSearchRequest request) {
        return tripClientService.searchTrips(request);
    }

    @GetMapping("/{tripId}")
    public TripResponse getTripDetails(@PathVariable Long tripId) {
        return tripClientService.getTripDetails(tripId);
    }

    @GetMapping("/{tripId}/available-seats")
    public TripAvailableSeatsResponse getAvailableSeats(
            @PathVariable Long tripId,
            @RequestParam @NotNull Long startTripStopId,
            @RequestParam @NotNull Long endTripStopId
    ) {
        return tripClientService.getAvailableSeats(tripId, startTripStopId, endTripStopId);
    }
}
