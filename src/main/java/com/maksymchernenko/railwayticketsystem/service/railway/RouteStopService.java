package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteStopResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RouteStopService {

    Page<RouteStopResponse> getRouteStops(Long routeId, Pageable pageable);

    RouteStopResponse createRouteStop(RouteStopCreateRequest request);

    void deleteRouteStop(Long id);
}
