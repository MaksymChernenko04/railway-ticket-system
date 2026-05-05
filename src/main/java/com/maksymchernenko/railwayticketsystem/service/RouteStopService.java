package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RouteStopService {

    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final StationRepository stationRepository;

    @Transactional(readOnly = true)
    public Page<RouteStopResponse> getRouteStops(Long routeId, Pageable pageable) {
        Page<RouteStop> routeStops = routeId == null
                ? routeStopRepository.findAll(pageable)
                : routeStopRepository.findByRoute_Id(routeId, pageable);

        return routeStops.map(this::toResponse);
    }

    @Transactional
    public RouteStopResponse createRouteStop(RouteStopCreateRequest request) {
        Route route = routeRepository.findById(request.routeId())
                .orElseThrow(() -> new EntityNotFoundException("Route not found: " + request.routeId()));

        Station station = stationRepository.findById(request.stationId())
                .orElseThrow(() -> new EntityNotFoundException("Station not found: " + request.stationId()));

        if (routeStopRepository.existsByRoute_IdAndOrdinal(route.getId(), request.ordinal())) {
            throw new EntityAlreadyExistsException("Route stop with this ordinal already exists");
        }

        if (routeStopRepository.existsByRoute_IdAndStation_Id(route.getId(), station.getId())) {
            throw new EntityAlreadyExistsException("Station already exists in this route");
        }

        RouteStop routeStop = new RouteStop();
        routeStop.setRoute(route);
        routeStop.setStation(station);
        routeStop.setOrdinal(request.ordinal());

        return toResponse(routeStopRepository.save(routeStop));
    }

    @Transactional
    public void deleteRouteStop(Long id) {
        routeStopRepository.deleteById(id);
    }

    private RouteStopResponse toResponse(RouteStop routeStop) {
        return new RouteStopResponse(
                routeStop.getId(),
                routeStop.getRoute().getId(),
                routeStop.getRoute().getNumber(),
                routeStop.getStation().getId(),
                routeStop.getStation().getName(),
                routeStop.getStation().getCountry().getCode(),
                routeStop.getOrdinal()
        );
    }
}
