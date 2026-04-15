package com.maksymchernenko.railwayticketsystem.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "ticket")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(optional = false)
    @JoinColumn(name = "passenger_id", nullable = false)
    private Passenger passenger;

    @ManyToOne(optional = false)
    @JoinColumn(name = "start_trip_stop_id", nullable = false)
    private TripStop startTripStop;

    @ManyToOne(optional = false)
    @JoinColumn(name = "end_trip_stop_id", nullable = false)
    private TripStop endTripStop;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_seat_id", nullable = false)
    private TripSeat tripSeat;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @ManyToOne(optional = false)
    @JoinColumn(name = "currency_code", nullable = false)
    private Currency currency;

    @Column(name = "applied_benefit_percent", precision = 5, scale = 2)
    private BigDecimal appliedBenefitPercent;

    @Column(name = "applied_benefit_code")
    private String appliedBenefitCode;

    public enum Status {
        ACTIVE,
        FINISHED,
        CANCELLED,
        RETURNED
    }
}
