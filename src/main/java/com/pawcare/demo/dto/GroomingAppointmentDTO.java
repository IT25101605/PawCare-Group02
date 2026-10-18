package com.pawcare.demo.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroomingAppointmentDTO {
    private Long id;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Pet is required")
    @Positive
    private Long petId;
    private String petName;

    private Long ownerId;
    private String ownerName;
    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Groomer is required")
    @Positive
    private Long groomerId;
    private String groomerName;
    private String groomerProfileImage;

    /** Backward-compatible primary service fields. */
    @Positive
    private Long serviceId;
    private String serviceName;

    /** Preferred multi-service fields. At least one service is enforced by the service layer on create. */
    @Size(max=8, message="Choose at most 8 grooming services")
    private List<@Positive Long> serviceIds;
    private List<String> serviceNames;
    private Integer totalDurationMinutes;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Date is required")
    private LocalDate appointmentDate;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Time is required")
    private LocalTime appointmentTime;

    @Pattern(regexp="SCHEDULED|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED")
    private String status;
    @Size(max=2000)
    private String specialInstructions;
    @Pattern(regexp="CALM|FIRST_VISIT|NERVOUS|", message="Choose a valid grooming comfort preference")
    private String comfortProfile;
    @Pattern(regexp="AS_DISCUSSED|NATURAL_TIDY|SHORT_NEAT|KEEP_LENGTH", message="Choose a valid finish preference")
    private String preferredFinish;
    @Size(max=10)
    private List<@NotBlank String> completedTasks;
    private List<com.pawcare.demo.service.GroomingVisitWorkflow.Task> visitTasks;
    private String progressLabel;
    private Boolean taskTrackingEnabled;
    @Size(max=500)
    private String productsUsed;
    private LocalDate nextVisitDate;
    private Boolean clearNextVisitDate;
    private BigDecimal totalPrice;
    @Size(max=2000)
    private String notes;
}
