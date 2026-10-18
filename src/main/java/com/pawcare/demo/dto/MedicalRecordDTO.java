package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalRecordDTO {
    private Long id;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Pet is required")
    @Positive
    private Long petId;
    private String petName;

    private Long veterinarianId;
    private String veterinarianName;
    private String ownerName;
    @Positive
    private Long appointmentId;
    private LocalDate appointmentDate;
    private LocalTime appointmentTime;
    private String appointmentReason;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Diagnosis is required")
    @Size(min=3,max=2000) @Pattern(regexp="(?s).*\\S.*")
    private String diagnosis;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Treatment is required")
    @Size(min=3,max=2000) @Pattern(regexp="(?s).*\\S.*")
    private String treatment;

    @Size(max=2000)
    private String prescription;
    @DecimalMin("0.01") @DecimalMax("999.99") @Digits(integer=3,fraction=2)
    private BigDecimal weightAtVisit;
    @DecimalMin("1") @DecimalMax("50") @Digits(integer=2,fraction=2)
    private BigDecimal temperature;
    @FutureOrPresent(groups=com.pawcare.demo.validation.OnCreate.class)
    private LocalDate followUpDate;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Record date is required")
    @PastOrPresent
    private LocalDate recordDate;
    @Pattern(regexp="CONSULTATION|VACCINATION|TREATMENT|PRESCRIPTION|LABORATORY|FOLLOW_UP|WEIGHT|GENERAL")
    private String recordType;
    @Size(max=2000)
    private String notes;
    private LocalDateTime createdAt;
    private Boolean clearWeightAtVisit;
    private Boolean clearTemperature;
    private Boolean clearFollowUpDate;
}
