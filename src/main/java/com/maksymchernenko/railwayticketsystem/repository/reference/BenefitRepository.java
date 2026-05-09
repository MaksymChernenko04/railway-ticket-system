package com.maksymchernenko.railwayticketsystem.repository.reference;

import com.maksymchernenko.railwayticketsystem.model.reference.Benefit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BenefitRepository extends JpaRepository<Benefit, String> {
}
