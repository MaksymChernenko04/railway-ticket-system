package com.maksymchernenko.railwayticketsystem.model.railway;

import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "trip_stop", uniqueConstraints = @UniqueConstraint(columnNames = {"trip_id", "route_stop_id"}))
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TripStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(optional = false)
    @JoinColumn(name = "route_stop_id", nullable = false)
    private RouteStop routeStop;

    @Column(nullable = false)
    private Integer platform;

    @Column(nullable = false)
    private Integer track;

    @Column(name = "arrival_offset_minutes", nullable = false)
    private Integer arrivalOffsetMinutes;

    @Column(name = "departure_offset_minutes", nullable = false)
    private Integer departureOffsetMinutes;
}
