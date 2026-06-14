package com.maksymchernenko.railwayticketsystem.service.railway;

import com.maksymchernenko.railwayticketsystem.dto.reference.CodeResponse;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.railway.RouteResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RouteService {

    Page<RouteResponse> getAllRoutes(Pageable pageable);

    List<CodeResponse> getCurrencies();

    RouteResponse createRoute(RouteCreateRequest request);

    void deleteRoute(Long id);
}
