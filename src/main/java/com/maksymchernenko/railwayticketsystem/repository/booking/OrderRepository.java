package com.maksymchernenko.railwayticketsystem.repository.booking;

import com.maksymchernenko.railwayticketsystem.model.booking.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByIdAndUserId(Long id, Long userId);

    Page<Order> findByUserIdOrderByCreatedAtUtcDescIdDesc(Long userId, Pageable pageable);

    List<Order> findByUserIdAndStatusAndExpiresAtUtcLessThanEqual(Long userId, Order.Status status, Instant expiresAtUtc);
}
