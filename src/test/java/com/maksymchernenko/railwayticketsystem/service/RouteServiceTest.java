package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.RouteResponse;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
public class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private RouteService routeService;

    @Test
    public void getAllRoutesShouldMapEntitiesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        Currency currency = new Currency();
        currency.setCode("PLN");

        Route route = new Route();
        route.setId(1L);
        route.setNumber("R1");
        route.setName("Route 1");
        route.setBasePrice(new BigDecimal("500.5"));
        route.setCurrency(currency);

        when(routeRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(route), pageable, 1));

        Page<RouteResponse> routeResponse = routeService.getAllRoutes(pageable);

        assertThat(routeResponse.getContent())
                .containsExactly(new RouteResponse(1L, "R1", "Route 1", new BigDecimal("500.5"), "PLN"));
    }
}
