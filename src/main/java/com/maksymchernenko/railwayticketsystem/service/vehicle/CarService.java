package com.maksymchernenko.railwayticketsystem.service.vehicle;

import com.maksymchernenko.railwayticketsystem.dto.vehicle.TypeResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CarService {

    Page<CarResponse> getAllCars(Pageable pageable);

    List<TypeResponse> getCarTypes();

    List<TypeResponse> getSeatTypes();

    CarResponse createCar(CarCreateRequest request);

    void deleteCar(Long id);

    List<SeatResponse> getSeatsByCarId(Long id);

    SeatResponse createSeat(Long carId, SeatCreateRequest request);

    void deleteSeat(Long carId, Long seatId);
}
