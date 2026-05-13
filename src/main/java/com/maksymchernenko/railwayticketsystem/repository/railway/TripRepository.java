package com.maksymchernenko.railwayticketsystem.repository.railway;

import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripRepository extends JpaRepository<Trip, Long> {
}
