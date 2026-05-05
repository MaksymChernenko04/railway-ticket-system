package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.RouteStop;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {

    Page<RouteStop> findByRoute_Id(Long routeId, Pageable pageable);

    boolean existsByRoute_IdAndOrdinal(Long routeId, Integer ordinal);

    boolean existsByRoute_IdAndStation_Id(Long routeId, Long stationId);
}
