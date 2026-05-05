package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
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

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteStopServiceTest {

    @Mock
    private RouteStopRepository routeStopRepository;

    @Mock
    private RouteRepository routeRepository;

    @Mock
    private StationRepository stationRepository;

    @InjectMocks
    private RouteStopService routeStopService;

    @Test
    void getRouteStopsShouldMapEntitiesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);
        RouteStop routeStop = createRouteStop();

        when(routeStopRepository.findByRoute_Id(1L, pageable))
                .thenReturn(new PageImpl<>(List.of(routeStop), pageable, 1));

        Page<RouteStopResponse> result = routeStopService.getRouteStops(1L, pageable);

        assertThat(result.getContent()).containsExactly(new RouteStopResponse(
                100L,
                1L,
                "R1",
                10L,
                "Kyiv",
                "UA",
                1
        ));
    }

    @Test
    void createRouteStopShouldSaveAndReturnResponse() {
        Route route = createRoute();
        Station station = createStation();
        RouteStop savedRouteStop = createRouteStop();
        RouteStopCreateRequest request = new RouteStopCreateRequest(1L, 10L, 1);

        when(routeRepository.findById(1L)).thenReturn(Optional.of(route));
        when(stationRepository.findById(10L)).thenReturn(Optional.of(station));
        when(routeStopRepository.existsByRoute_IdAndOrdinal(1L, 1)).thenReturn(false);
        when(routeStopRepository.existsByRoute_IdAndStation_Id(1L, 10L)).thenReturn(false);
        when(routeStopRepository.save(any(RouteStop.class))).thenReturn(savedRouteStop);

        RouteStopResponse result = routeStopService.createRouteStop(request);

        ArgumentCaptor<RouteStop> routeStopCaptor = ArgumentCaptor.forClass(RouteStop.class);
        verify(routeStopRepository).save(routeStopCaptor.capture());
        RouteStop routeStopToSave = routeStopCaptor.getValue();

        assertThat(routeStopToSave.getRoute()).isSameAs(route);
        assertThat(routeStopToSave.getStation()).isSameAs(station);
        assertThat(routeStopToSave.getOrdinal()).isEqualTo(1);
        assertThat(result).isEqualTo(new RouteStopResponse(100L, 1L, "R1", 10L, "Kyiv", "UA", 1));
    }

    @Test
    void createRouteStopShouldThrowWhenRouteDoesNotExist() {
        RouteStopCreateRequest request = new RouteStopCreateRequest(1L, 10L, 1);

        when(routeRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> routeStopService.createRouteStop(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Route not found: 1");

        verify(stationRepository, never()).findById(any());
        verify(routeStopRepository, never()).save(any());
    }

    @Test
    void createRouteStopShouldThrowWhenOrdinalAlreadyExists() {
        RouteStopCreateRequest request = new RouteStopCreateRequest(1L, 10L, 1);

        when(routeRepository.findById(1L)).thenReturn(Optional.of(createRoute()));
        when(stationRepository.findById(10L)).thenReturn(Optional.of(createStation()));
        when(routeStopRepository.existsByRoute_IdAndOrdinal(1L, 1)).thenReturn(true);

        assertThatThrownBy(() -> routeStopService.createRouteStop(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Route stop with this ordinal already exists");

        verify(routeStopRepository, never()).save(any());
    }

    @Test
    void createRouteStopShouldThrowWhenStationAlreadyExistsInRoute() {
        RouteStopCreateRequest request = new RouteStopCreateRequest(1L, 10L, 1);

        when(routeRepository.findById(1L)).thenReturn(Optional.of(createRoute()));
        when(stationRepository.findById(10L)).thenReturn(Optional.of(createStation()));
        when(routeStopRepository.existsByRoute_IdAndOrdinal(1L, 1)).thenReturn(false);
        when(routeStopRepository.existsByRoute_IdAndStation_Id(1L, 10L)).thenReturn(true);

        assertThatThrownBy(() -> routeStopService.createRouteStop(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Station already exists in this route");

        verify(routeStopRepository, never()).save(any());
    }

    private RouteStop createRouteStop() {
        RouteStop routeStop = new RouteStop();
        routeStop.setId(100L);
        routeStop.setRoute(createRoute());
        routeStop.setStation(createStation());
        routeStop.setOrdinal(1);
        return routeStop;
    }

    private Route createRoute() {
        Route route = new Route();
        route.setId(1L);
        route.setNumber("R1");
        return route;
    }

    private Station createStation() {
        Country country = new Country();
        country.setCode("UA");

        Station station = new Station();
        station.setId(10L);
        station.setName("Kyiv");
        station.setCountry(country);
        return station;
    }
}
