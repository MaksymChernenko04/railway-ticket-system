package com.maksymchernenko.railwayticketsystem.repository.vehicle;

import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SeatRepository extends JpaRepository<Seat, Long> {
    List<Seat> findAllByCar_Id(Long carId);
}
