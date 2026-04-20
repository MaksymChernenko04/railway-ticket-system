package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.RouteResponse;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteService {

    private final RouteRepository routeRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        return null;
    }

    @Transactional
    public void deleteRoute(Long id) {

    }

    private RouteResponse toResponse(Route route) {
        return new RouteResponse(
                route.getId(),
                route.getNumber(),
                route.getName(),
                route.getBasePrice(),
                route.getCurrency().getCode()
        );
    }
}
