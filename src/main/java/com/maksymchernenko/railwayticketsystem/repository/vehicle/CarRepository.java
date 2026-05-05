package com.maksymchernenko.railwayticketsystem.repository.vehicle;

import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarRepository extends JpaRepository<Car, Long> {
    List<Car> findByNumber(String number);
}
