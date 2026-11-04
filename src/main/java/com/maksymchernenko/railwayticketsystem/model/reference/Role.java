package com.maksymchernenko.railwayticketsystem.model.reference;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "role")
@NoArgsConstructor
@Getter
@Setter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class Role {

    @Id
    @Column(length = 30)
    @EqualsAndHashCode.Include
    private String name;
}
