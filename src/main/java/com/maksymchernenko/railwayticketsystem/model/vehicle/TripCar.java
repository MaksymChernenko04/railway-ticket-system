package com.maksymchernenko.railwayticketsystem.model.vehicle;

import com.maksymchernenko.railwayticketsystem.model.railway.Trip;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Entity
@Table(name = "trip_car", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"trip_id", "car_id"}),
        @UniqueConstraint(columnNames = {"trip_id", "ordinal"})
})
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class TripCar {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(nullable = false)
    private Integer ordinal;

    @OneToMany(mappedBy = "tripCar", cascade = CascadeType.ALL)
    private List<TripSeat> tripSeats;
}
