package com.maksymchernenko.railwayticketsystem.repository.vehicle;

import com.maksymchernenko.railwayticketsystem.model.vehicle.TripCar;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripCarRepository extends JpaRepository<TripCar, Long> {
}
