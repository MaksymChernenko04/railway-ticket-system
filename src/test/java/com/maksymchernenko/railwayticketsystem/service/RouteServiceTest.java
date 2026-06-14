package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.CurrencyRepository;
import com.maksymchernenko.railwayticketsystem.service.railway.RouteServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RouteServiceTest {

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private CurrencyRepository currencyRepository;

    @InjectMocks
    private RouteServiceImpl routeService;

    @Test
    void getAllRoutesShouldMapEntitiesToResponses() {
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

    @Test
    void createRouteShouldTrimValuesNormalizeCurrencyCodeAndReturnResponse() {
        RouteCreateRequest request = new RouteCreateRequest(
                "  R1  ",
                "  Route 1  ",
                new BigDecimal("500.5"),
                " pln "
        );

        Currency currency = new Currency();
        currency.setCode("PLN");

        Route savedRoute = new Route();
        savedRoute.setId(1L);
        savedRoute.setNumber("R1");
        savedRoute.setName("Route 1");
        savedRoute.setBasePrice(new BigDecimal("500.5"));
        savedRoute.setCurrency(currency);

        when(routeRepository.existsByNumberIgnoreCase("R1")).thenReturn(false);
        when(currencyRepository.findById("PLN")).thenReturn(Optional.of(currency));
        when(routeRepository.save(any(Route.class))).thenReturn(savedRoute);

        RouteResponse result = routeService.createRoute(request);

        ArgumentCaptor<Route> routeCaptor = ArgumentCaptor.forClass(Route.class);
        verify(routeRepository).save(routeCaptor.capture());
        Route routeToSave = routeCaptor.getValue();

        assertThat(routeToSave.getNumber()).isEqualTo("R1");
        assertThat(routeToSave.getName()).isEqualTo("Route 1");
        assertThat(routeToSave.getBasePrice()).isEqualTo(new BigDecimal("500.5"));
        assertThat(routeToSave.getCurrency()).isSameAs(currency);
        assertThat(result).isEqualTo(new RouteResponse(
                1L,
                "R1",
                "Route 1",
                new BigDecimal("500.5"),
                "PLN"));
    }

    @Test
    void createRouteShouldThrowWhenRouteAlreadyExists() {
        RouteCreateRequest request = new RouteCreateRequest(
                "R1",
                "Route 1",
                new BigDecimal("500.5"),
                "PLN"
        );

        when(routeRepository.existsByNumberIgnoreCase("R1")).thenReturn(true);

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Route with this number already exists");

        verify(currencyRepository, never()).findById(any());
        verify(routeRepository, never()).save(any());
    }

    @Test
    void createRouteShouldThrowWhenCurrencyDoesNotExist() {
        RouteCreateRequest request = new RouteCreateRequest(
                "R1",
                "Route 1",
                new BigDecimal("500.5"),
                "PLN"
        );

        when(routeRepository.existsByNumberIgnoreCase("R1")).thenReturn(false);
        when(currencyRepository.findById("PLN")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeService.createRoute(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Currency not found: PLN");

        verify(routeRepository, never()).save(any());
    }
}
