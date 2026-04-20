package com.maksymchernenko.railwayticketsystem.repository.reference;

import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CountryRepository extends JpaRepository<Country, String> {
}
