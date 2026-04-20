package com.maksymchernenko.railwayticketsystem.repository.reference;

import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CurrencyRepository extends JpaRepository<Currency, String> {
}
