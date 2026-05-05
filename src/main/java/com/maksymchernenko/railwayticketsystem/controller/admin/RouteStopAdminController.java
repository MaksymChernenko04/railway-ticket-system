package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopResponse;
import com.maksymchernenko.railwayticketsystem.service.RouteStopService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/route-stops")
@RequiredArgsConstructor
public class RouteStopAdminController {

    private final RouteStopService routeStopService;

    @GetMapping
    public Page<RouteStopResponse> getRouteStops(
            @RequestParam(required = false) Long routeId,
            Pageable pageable
    ) {
        return routeStopService.getRouteStops(routeId, pageable);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteStopResponse createRouteStop(@Valid @RequestBody RouteStopCreateRequest request) {
        return routeStopService.createRouteStop(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRouteStop(@PathVariable Long id) {
        routeStopService.deleteRouteStop(id);
    }
}
