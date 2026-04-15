package com.maksymchernenko.railwayticketsystem.model;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Entity
@Table(name = "trip")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(optional = false)
    @JoinColumn(name = "train_id", nullable = false)
    private Train train;

    @Column(name = "departure_at_utc", nullable = false)
    private Instant departureAtUtc;

    @OneToMany(mappedBy = "tripStop", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<TripStop> tripStops;

    @OneToMany(mappedBy = "tripCar", cascade = CascadeType.ALL)
    private List<TripCar> tripCars;
}
