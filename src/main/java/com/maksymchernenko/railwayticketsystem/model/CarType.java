package com.maksymchernenko.railwayticketsystem.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "car_type")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class CarType {

    @Id
    @EqualsAndHashCode.Include
    private String code;

    @Column(name = "price_multiplier", nullable = false, precision = 5, scale = 2)
    private BigDecimal priceMultiplier;
}
