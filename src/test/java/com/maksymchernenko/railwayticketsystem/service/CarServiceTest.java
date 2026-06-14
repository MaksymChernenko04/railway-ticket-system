package com.maksymchernenko.railwayticketsystem.service;

import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.CarResponse;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatCreateRequest;
import com.maksymchernenko.railwayticketsystem.dto.vehicle.SeatResponse;
import com.maksymchernenko.railwayticketsystem.model.reference.CarType;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Car;
import com.maksymchernenko.railwayticketsystem.model.vehicle.Seat;
import com.maksymchernenko.railwayticketsystem.repository.reference.CarTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.reference.SeatTypeRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.CarRepository;
import com.maksymchernenko.railwayticketsystem.repository.vehicle.SeatRepository;
import com.maksymchernenko.railwayticketsystem.service.vehicle.CarServiceImpl;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maksymchernenko.railwayticketsystem.exception.railway.EntityAlreadyExistsException;
import com.maksymchernenko.railwayticketsystem.exception.railway.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CarServiceTest {

    @Mock
    private CarRepository carRepository;

    @Mock
    private CarTypeRepository carTypeRepository;

    @Mock
    private SeatRepository seatRepository;

    @Mock
    private SeatTypeRepository seatTypeRepository;

    @InjectMocks
    private CarServiceImpl carService;

    @Test
    void getAllCarsShouldMapEntitiesToResponses() {
        Pageable pageable = PageRequest.of(0, 2);

        CarType type = new CarType();
        type.setCode("STANDARD");

        Car car = new Car();
        car.setId(1L);
        car.setType(type);
        car.setNumber("C1");
        car.setAccessible(true);

        when(carRepository.findAll(pageable))
                .thenReturn(new PageImpl<>(List.of(car), pageable, 1));

        Page<CarResponse> result = carService.getAllCars(pageable);

        assertThat(result.getContent())
                .containsExactly(new CarResponse(1L, "STANDARD", "C1", true));

        verify(carRepository).findAll(pageable);
    }

    @Test
    void createCarShouldNormalizeAndReturnResponse() {
        CarCreateRequest request = new CarCreateRequest(" standard ", "  C1  ", true);

        CarType type = new CarType();
        type.setCode("STANDARD");

        Car savedCar = new Car();
        savedCar.setId(1L);
        savedCar.setType(type);
        savedCar.setNumber("C1");
        savedCar.setAccessible(true);

        when(carTypeRepository.findById("STANDARD")).thenReturn(Optional.of(type));
        when(carRepository.findByNumber("C1")).thenReturn(List.of());
        when(carRepository.save(any(Car.class))).thenReturn(savedCar);

        CarResponse result = carService.createCar(request);

        ArgumentCaptor<Car> captor = ArgumentCaptor.forClass(Car.class);
        verify(carRepository).save(captor.capture());

        Car toSave = captor.getValue();

        assertThat(toSave.getNumber()).isEqualTo("C1");
        assertThat(toSave.getType()).isSameAs(type);
        assertThat(toSave.getAccessible()).isTrue();

        assertThat(result)
                .isEqualTo(new CarResponse(1L, "STANDARD", "C1", true));
    }

    @Test
    void createCarShouldThrowWhenTypeNotFound() {
        CarCreateRequest request = new CarCreateRequest("standard", "C1", true);

        when(carTypeRepository.findById("STANDARD")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.createCar(request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car type not found: STANDARD");

        verify(carRepository, never()).save(any());
    }

    @Test
    void createCarShouldThrowWhenCarAlreadyExists() {
        CarCreateRequest request = new CarCreateRequest("standard", "C1", true);

        CarType type = new CarType();
        type.setCode("STANDARD");

        when(carTypeRepository.findById("STANDARD")).thenReturn(Optional.of(type));
        when(carRepository.findByNumber("C1")).thenReturn(List.of(new Car()));

        assertThatThrownBy(() -> carService.createCar(request))
                .isInstanceOf(EntityAlreadyExistsException.class)
                .hasMessage("Car with number C1 already exists");

        verify(carRepository, never()).save(any());
    }

    @Test
    void getSeatsByCarIdShouldMapSeats() {
        SeatType type = new SeatType();
        type.setCode("WINDOW");

        Seat seat = new Seat();
        seat.setId(1L);
        seat.setType(type);
        seat.setNumber("1A");
        seat.setRow(1);
        seat.setColumn(1);
        seat.setDirection(SeatDirection.FORWARD);

        when(seatRepository.findAllByCarId(1L)).thenReturn(List.of(seat));

        List<SeatResponse> result = carService.getSeatsByCarId(1L);

        assertThat(result).containsExactly(
                new SeatResponse(1L, "WINDOW", "1A", 1, 1, SeatDirection.FORWARD)
        );
    }

    @Test
    void createSeatShouldCreateAndReturnResponse() {
        SeatCreateRequest request = new SeatCreateRequest(
                " window ",
                " 1A ",
                1,
                1,
                SeatDirection.FORWARD
        );

        Car car = new Car();
        car.setId(1L);

        SeatType type = new SeatType();
        type.setCode("WINDOW");

        Seat savedSeat = new Seat();
        savedSeat.setId(10L);
        savedSeat.setCar(car);
        savedSeat.setType(type);
        savedSeat.setNumber("1A");
        savedSeat.setRow(1);
        savedSeat.setColumn(1);
        savedSeat.setDirection(SeatDirection.FORWARD);

        when(carRepository.findById(1L)).thenReturn(Optional.of(car));
        when(seatTypeRepository.findById("WINDOW")).thenReturn(Optional.of(type));
        when(seatRepository.save(any())).thenReturn(savedSeat);

        SeatResponse result = carService.createSeat(1L, request);

        assertThat(result)
                .isEqualTo(new SeatResponse(10L, "WINDOW", "1A", 1, 1, SeatDirection.FORWARD));
    }

    @Test
    void createSeatShouldThrowWhenCarNotFound() {
        SeatCreateRequest request = new SeatCreateRequest("WINDOW", "1A", 1, 1, SeatDirection.FORWARD);

        when(carRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> carService.createSeat(1L, request))
                .isInstanceOf(EntityNotFoundException.class)
                .hasMessage("Car not found: 1");

        verify(seatRepository, never()).save(any());
    }

    @Test
    void deleteSeatShouldDeleteWhenBelongsToCar() {
        Car car = new Car();
        car.setId(1L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setCar(car);

        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));

        carService.deleteSeat(1L, 10L);

        verify(seatRepository).delete(seat);
    }

    @Test
    void deleteSeatShouldThrowWhenSeatDoesNotBelongToCar() {
        Car car = new Car();
        car.setId(2L);

        Seat seat = new Seat();
        seat.setId(10L);
        seat.setCar(car);

        when(seatRepository.findById(10L)).thenReturn(Optional.of(seat));

        assertThatThrownBy(() -> carService.deleteSeat(1L, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Seat does not belong to this car");

        verify(seatRepository, never()).delete(any());
    }
}
