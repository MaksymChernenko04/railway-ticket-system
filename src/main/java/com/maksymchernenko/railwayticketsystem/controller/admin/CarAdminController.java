package com.maksymchernenko.railwayticketsystem.controller.admin;

import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.TypeResponse;
import com.maksymchernenko.railwayticketsystem.service.CarService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cars")
@RequiredArgsConstructor
public class CarAdminController {

    private final CarService carService;

    @GetMapping
    public Page<CarResponse> getCars(Pageable pageable) {
        return carService.getAllCars(pageable);
    }

    @GetMapping("/types")
    public List<TypeResponse> getCarTypes() {
        return carService.getCarTypes();
    }

    @GetMapping("/seat-types")
    public List<TypeResponse> getSeatTypes() {
        return carService.getSeatTypes();
    }

    @PostMapping
    public CarResponse createCar(@Valid @RequestBody CarCreateRequest request) {
        return carService.createCar(request);
    }

    @DeleteMapping("/{id}")
    public void deleteCar(@PathVariable Long id) {
        carService.deleteCar(id);
    }

    @GetMapping("/{carId}/seats")
    public List<SeatResponse> getSeats(@PathVariable Long carId) {
        return carService.getSeatsByCarId(carId);
    }

    @PostMapping("/{carId}/seats")
    public SeatResponse createSeat(@PathVariable Long carId,
                                   @Valid @RequestBody SeatCreateRequest request) {
        return carService.createSeat(carId, request);
    }

    @DeleteMapping("/{carId}/seats/{seatId}")
    public void deleteSeat(@PathVariable Long carId,
                           @PathVariable Long seatId) {
        carService.deleteSeat(carId, seatId);
    }
}
