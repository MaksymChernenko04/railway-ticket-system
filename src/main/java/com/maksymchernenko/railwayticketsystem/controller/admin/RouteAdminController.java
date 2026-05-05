package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.reference.CodeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteResponse;
import com.maksymchernenko.railwayticketsystem.service.RouteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/routes")
@RequiredArgsConstructor
public class RouteAdminController {

    private final RouteService routeService;

    @GetMapping
    public Page<RouteResponse> getRoutes(Pageable pageable) {
        return routeService.getAllRoutes(pageable);
    }

    @GetMapping("/currencies")
    public List<CodeResponse> getCurrencies() {
        return routeService.getCurrencies();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RouteResponse createStation(@Valid @RequestBody RouteCreateRequest request) {
        return routeService.createRoute(request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteRoute(@PathVariable Long id) {
        routeService.deleteRoute(id);
    }
}
