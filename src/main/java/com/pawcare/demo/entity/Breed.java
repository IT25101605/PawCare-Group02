package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "breeds")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Breed {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @Column(length = 255)
    private String description;
}
