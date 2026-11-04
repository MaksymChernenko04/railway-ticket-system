package com.maksymchernenko.railwayticketsystem.repository.vehicle;

import com.maksymchernenko.railwayticketsystem.model.vehicle.TripSeat;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TripSeatRepository extends JpaRepository<TripSeat, Long> {
}
