package com.maksymchernenko.railwayticketsystem.model.railway;

import com.maksymchernenko.railwayticketsystem.model.reference.Country;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "station", uniqueConstraints = @UniqueConstraint(columnNames = {"country_code", "name"}))
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @Column(nullable = false, length = 255)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "country_code", nullable = false)
    private Country country;

    @Column(nullable = false, length = 50)
    private String timezone;

    @Column(length = 255)
    private String address;

    @Column(name = "is_accessible", nullable = false)
    private Boolean accessible;
}
