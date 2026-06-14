package com.maksymchernenko.railwayticketsystem.repository.booking;

import com.maksymchernenko.railwayticketsystem.model.booking.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    List<Payment> findByOrderIdOrderByIdAsc(Long orderId);

    Optional<Payment> findByIdAndOrderUserId(Long id, Long userId);
}
