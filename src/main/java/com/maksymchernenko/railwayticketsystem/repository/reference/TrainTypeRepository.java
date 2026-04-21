package com.maksymchernenko.railwayticketsystem.repository.reference;

import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrainTypeRepository extends JpaRepository<TrainType, String> {
}
