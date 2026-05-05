package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatResponse;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import com.maksymchernenko.railwayticketsystem.model.reference.CarType;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import com.maksymchernenko.railwayticketsystem.repository.reference.CarTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.SeatTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.CarRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.SeatRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CarService {

    private final CarRepository carRepository;
    private final CarTypeRepository carTypeRepository;
    private final SeatRepository seatRepository;
    private final SeatTypeRepository seatTypeRepository;

    @Transactional(readOnly = true)
    public Page<CarResponse> getAllCars(Pageable pageable) {
        return carRepository.findAll(pageable)
                .map(this::toCarResponse);
    }

    @Transactional
    public CarResponse createCar(CarCreateRequest request) {
        String normalizedTypeCode = request.typeCode().trim().toUpperCase();
        String normalizedNumber = request.number().trim();

        CarType type = carTypeRepository.findById(normalizedTypeCode)
                .orElseThrow(() -> new EntityNotFoundException("Car type not found: " + normalizedTypeCode));

        if (!carRepository.findByNumber(normalizedNumber).isEmpty()) {
            throw new EntityAlreadyExistsException("Car with number " + normalizedNumber + " already exists");
        }

        Car car = new Car();
        car.setType(type);
        car.setNumber(normalizedNumber);
        car.setAccessible(request.accessible());

        return toCarResponse(carRepository.save(car));
    }

    @Transactional
    public void deleteCar(Long id) {
        carRepository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public List<SeatResponse> getSeatsByCarId(Long id) {
        return seatRepository.findAllByCar_Id(id).stream()
                .map(this::toSeatResponse)
                .toList();
    }

    @Transactional
    public SeatResponse createSeat(Long carId, SeatCreateRequest request) {
        Car car = carRepository.findById(carId)
                .orElseThrow(() -> new EntityNotFoundException("Car not found: " + carId));

        String normalizedSeatType = request.typeCode().trim().toUpperCase();

        SeatType seatType = seatTypeRepository.findById(normalizedSeatType)
                .orElseThrow(() -> new EntityNotFoundException("Seat type not found: " + normalizedSeatType));

        Seat seat = new Seat();
        seat.setCar(car);
        seat.setType(seatType);
        seat.setNumber(request.number().trim());
        seat.setRow(request.row());
        seat.setColumn(request.column());
        seat.setDirection(request.direction());

        return toSeatResponse(seatRepository.save(seat));
    }

    @Transactional
    public void deleteSeat(Long carId, Long seatId) {
        Seat seat = seatRepository.findById(seatId)
                .orElseThrow(() -> new EntityNotFoundException("Seat not found: " + seatId));

        if (!seat.getCar().getId().equals(carId)) {
            throw new IllegalArgumentException("Seat does not belong to this car");
        }

        seatRepository.delete(seat);
    }

    private CarResponse toCarResponse(Car car) {
        return new CarResponse(
                car.getId(),
                car.getType().getCode(),
                car.getNumber(),
                car.getAccessible()
        );
    }

    private SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getType().getCode(),
                seat.getNumber(),
                seat.getRow(),
                seat.getColumn(),
                seat.getDirection()
        );
    }
}
