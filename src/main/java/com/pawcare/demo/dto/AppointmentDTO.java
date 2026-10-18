package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class AppointmentDTO {
    private Long id;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Pet is required")
    @Positive
    private Long petId;
    private String petName;

    private Long ownerId;
    private String ownerName;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Veterinarian is required")
    @Positive
    private Long veterinarianId;
    private String veterinarianName;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Appointment date is required")
    private LocalDate appointmentDate;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Appointment time is required")
    private LocalTime appointmentTime;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Reason is required")
    @Size(min=3,max=1000) @Pattern(regexp="(?s).*\\S.*",message="Reason cannot be blank")
    private String reason;

    @Pattern(regexp="SCHEDULED|CONFIRMED|IN_PROGRESS|COMPLETED|CANCELLED|NO_SHOW")
    private String status;
    @Size(max=2000)
    private String notes;
}
