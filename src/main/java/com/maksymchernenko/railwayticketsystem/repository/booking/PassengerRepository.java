package com.maksymchernenko.railwayticketsystem.repository.booking;

import com.maksymchernenko.railwayticketsystem.model.booking.Passenger;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PassengerRepository extends JpaRepository<Passenger, Long> {

    List<Passenger> findByUserIdOrderByIdAsc(Long userId);

    Optional<Passenger> findByIdAndUserId(Long id, Long userId);
}
