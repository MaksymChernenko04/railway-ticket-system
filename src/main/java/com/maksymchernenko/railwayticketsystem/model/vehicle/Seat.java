package com.maksymchernenko.railwayticketsystem.model.vehicle;

import com.maksymchernenko.railwayticketsystem.model.reference.SeatDirection;
import com.maksymchernenko.railwayticketsystem.model.reference.SeatType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "seat", uniqueConstraints = @UniqueConstraint(columnNames = {"car_id", "number"}))
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_code", nullable = false)
    private SeatType type;

    @ManyToOne(optional = false)
    @JoinColumn(name = "car_id", nullable = false)
    private Car car;

    @Column(nullable = false)
    private String number;

    @Column(name = "`row`", nullable = false)
    private Integer row;

    @Column(name = "`column`", nullable = false)
    private Integer column;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatDirection direction;
}
