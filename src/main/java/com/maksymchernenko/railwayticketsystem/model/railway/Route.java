package com.maksymchernenko.railwayticketsystem.model.railway;

import com.maksymchernenko.railwayticketsystem.model.reference.Currency;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Entity
@Table(name = "route")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Route {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, unique = true)
    private String number;

    private String name;

    @Column(name = "base_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @ManyToOne(optional = false)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @OneToMany(mappedBy = "route", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<RouteStop> routeStops;
}
