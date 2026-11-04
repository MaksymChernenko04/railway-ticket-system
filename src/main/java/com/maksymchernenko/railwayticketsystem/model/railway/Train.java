package com.maksymchernenko.railwayticketsystem.model.railway;

import com.maksymchernenko.railwayticketsystem.model.reference.TrainType;
import jakarta.persistence.*;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "train")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Train {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @EqualsAndHashCode.Include
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_code", nullable = false)
    private TrainType type;

    @Column(nullable = false, unique = true, length = 50)
    private String number;

    @Column(length = 100)
    private String name;
}
