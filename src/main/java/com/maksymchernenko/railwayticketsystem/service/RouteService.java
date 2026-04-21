package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.RouteResponse;
import com.maksymchernenko.railwayticketsystem.exception.CurrencyNotFoundException;
import com.maksymchernenko.railwayticketsystem.exception.RouteAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

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
        String normalizedCurrencyCode = request.currencyCode().trim().toUpperCase(Locale.ROOT);
        String normalizedNumber = request.number().trim();

        if (routeRepository.existsByNumberIgnoreCase(normalizedNumber)) {
            throw new RouteAlreadyExistsException("Route with this number already exists");
        }

        Currency currency = currencyRepository.findById(normalizedCurrencyCode)
                .orElseThrow(() -> new CurrencyNotFoundException("Currency not found: " + normalizedCurrencyCode));

        Route route = new Route();
        route.setNumber(normalizedNumber);
        route.setName(request.name().trim());
        route.setBasePrice(request.basePrice());
        route.setCurrency(currency);

        return toResponse(routeRepository.save(route));
    }

    @Transactional
    public void deleteRoute(Long id) {
        routeRepository.deleteById(id);
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
