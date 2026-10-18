package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "species_id", nullable = false)
    private Species species;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "breed_id")
    private Breed breed;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    @Builder.Default private Gender gender = Gender.UNKNOWN;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(length = 50)
    private String color;

    @Column(name = "microchip_id", length = 50)
    private String microchipId;

    @Column(length = 500)
    private String photo;

    @Column(columnDefinition = "TEXT")
    private String notes;

    // Digital Pet Passport settings. Existing pets are backfilled at startup.
    @Column(name = "passport_token", unique = true, length = 36)
    private String passportToken;

    @Column(name = "passport_enabled")
    @Builder.Default private Boolean passportEnabled = false;

    @Column(name = "passport_show_photo")
    @Builder.Default private Boolean passportShowPhoto = true;

    @Column(name = "passport_show_owner_contact")
    @Builder.Default private Boolean passportShowOwnerContact = false;

    @Column(name = "passport_show_allergies")
    @Builder.Default private Boolean passportShowAllergies = true;

    @Column(name = "passport_show_medical_warnings")
    @Builder.Default private Boolean passportShowMedicalWarnings = true;

    @Column(name = "passport_show_vaccinations")
    @Builder.Default private Boolean passportShowVaccinations = true;

    @Column(name = "passport_show_emergency_vet")
    @Builder.Default private Boolean passportShowEmergencyVet = true;

    @Column(name = "passport_allergies", columnDefinition = "TEXT")
    private String passportAllergies;

    @Column(name = "passport_medical_warnings", columnDefinition = "TEXT")
    private String passportMedicalWarnings;

    @Column(name = "passport_emergency_vet_id")
    private Long passportEmergencyVetId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(name = "is_active")
    @Builder.Default private Boolean isActive = true;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { if (passportToken == null || passportToken.isBlank()) passportToken = java.util.UUID.randomUUID().toString(); createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum Gender { MALE, FEMALE, UNKNOWN }
}
