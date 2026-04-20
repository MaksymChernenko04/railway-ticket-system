package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Route;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RouteRepository extends JpaRepository<Route, Integer> {
    boolean existsByNumberIgnoreCase(String number);
}
