package com.maksymchernenko.railwayticketsystem.service.ai.context;

import com.maksymchernenko.railwayticketsystem.dto.ai.context.RouteContext;
import com.maksymchernenko.railwayticketsystem.dto.ai.context.RouteStopContext;
import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import com.maksymchernenko.railwayticketsystem.model.railway.Station;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.RouteStopRepository;
import com.maksymchernenko.railwayticketsystem.repository.railway.StationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AssistantCatalogService {

    private final StationRepository stationRepository;
    private final RouteRepository routeRepository;
    private final RouteStopRepository routeStopRepository;

    @Transactional(readOnly = true)
    public String buildCatalogPrompt() {
        String stations = stationRepository.findAll().stream()
                .sorted(Comparator.comparing(Station::getName))
                .map(station -> "%s [%s]".formatted(station.getName(), station.getCountry().getCode()))
                .collect(Collectors.joining("; "));

        String routes = routeRepository.findAll().stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .map(route -> "routeId=%s, number=%s, name=%s, stops=%s".formatted(
                        route.getId(),
                        route.getNumber(),
                        route.getName(),
                        routeStopsText(route.getId())
                ))
                .collect(Collectors.joining("\n"));

        return """
                Available stations from the database:
                %s

                Available route network from the database:
                %s
                """.formatted(
                stations.isBlank() ? "No stations configured." : stations,
                routes.isBlank() ? "No routes configured." : routes
        );
    }

    @Transactional(readOnly = true)
    public List<RouteContext> getRouteNetwork(int limit) {
        return routeRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(Route::getNumber))
                .limit(limit)
                .map(this::toRouteContext)
                .toList();
    }

    public RouteContext toRouteContext(Route route) {
        List<RouteStopContext> stops = routeStopRepository.findByRouteId(route.getId())
                .stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .map(stop -> new RouteStopContext(
                        stop.getStation().getId(),
                        stop.getStation().getName(),
                        stop.getStation().getCountry().getCode(),
                        stop.getStation().getTimezone(),
                        stop.getOrdinal()
                ))
                .toList();

        return new RouteContext(
                route.getId(),
                route.getNumber(),
                route.getName(),
                route.getBasePrice(),
                route.getCurrency().getCode(),
                stops
        );
    }

    private String routeStopsText(Long routeId) {
        String stops = routeStopRepository.findByRouteId(routeId).stream()
                .sorted(Comparator.comparing(RouteStop::getOrdinal))
                .map(stop -> stop.getStation().getName())
                .collect(Collectors.joining(" -> ", "", ""));
        return stops.isBlank() ? "no stops" : stops;
    }
}
