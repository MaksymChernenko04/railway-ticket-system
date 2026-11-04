package com.maksymchernenko.railwayticketsystem.model.reference;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "benefit")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Benefit {

    @Id
    @Column(length = 30)
    @EqualsAndHashCode.Include
    private String code;

    @Column(name = "percent_amount", nullable = false, precision = 5, scale = 2)
    private BigDecimal percentAmount;
}
