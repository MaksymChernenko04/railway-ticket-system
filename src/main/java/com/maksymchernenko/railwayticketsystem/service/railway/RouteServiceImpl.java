package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.reference.CodeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private final RouteRepository routeRepository;
    private final CurrencyRepository currencyRepository;

    @Transactional(readOnly = true)
    public Page<RouteResponse> getAllRoutes(Pageable pageable) {
        return routeRepository.findAll(pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public List<CodeResponse> getCurrencies() {
        return currencyRepository.findAll().stream()
                .sorted(Comparator.comparing(Currency::getCode))
                .map(currency -> new CodeResponse(currency.getCode()))
                .toList();
    }

    @Transactional
    public RouteResponse createRoute(RouteCreateRequest request) {
        String normalizedCurrencyCode = request.currencyCode().trim().toUpperCase(Locale.ROOT);
        String normalizedNumber = request.number().trim();

        if (routeRepository.existsByNumberIgnoreCase(normalizedNumber)) {
            throw new EntityAlreadyExistsException("Route with this number already exists");
        }

        Currency currency = currencyRepository.findById(normalizedCurrencyCode)
                .orElseThrow(() -> new EntityNotFoundException("Currency not found: " + normalizedCurrencyCode));

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
