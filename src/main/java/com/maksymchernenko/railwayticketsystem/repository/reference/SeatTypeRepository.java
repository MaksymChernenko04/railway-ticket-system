package com.maksymchernenko.railwayticketsystem.repository.reference;

import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SeatTypeRepository extends JpaRepository<SeatType, String> {
}
