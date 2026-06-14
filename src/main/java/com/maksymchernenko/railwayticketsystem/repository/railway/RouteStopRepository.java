package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    Page<RouteStop> findByRouteId(Long routeId, Pageable pageable);

    boolean existsByRouteIdAndOrdinal(Long routeId, Integer ordinal);

    boolean existsByRouteIdAndStationId(Long routeId, Long stationId);

    List<RouteStop> findByRouteId(Long routeId);

    List<RouteStop> findByStationId(Long stationId);

    List<RouteStop> findByRouteIdOrderByOrdinalDesc(Long routeId);
}
