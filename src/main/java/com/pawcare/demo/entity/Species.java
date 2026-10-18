package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "species")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Species {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(length = 255)
    private String description;
}
