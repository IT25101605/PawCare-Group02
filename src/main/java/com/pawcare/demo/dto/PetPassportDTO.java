package com.pawcare.demo.dto;

import jakarta.validation.constraints.Size;
import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PetPassportDTO {
    private Long petId;
    private String petName;
    private String passportToken;
    private Boolean enabled;
    private Boolean showPhoto;
    private Boolean showOwnerContact;
    private Boolean showAllergies;
    private Boolean showMedicalWarnings;
    private Boolean showVaccinations;
    private Boolean showEmergencyVet;
    @Size(max = 2000) private String allergies;
    @Size(max = 2000) private String medicalWarnings;
    private Long emergencyVetId;
}
