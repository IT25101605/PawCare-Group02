package com.pawcare.demo.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PublicPetPassportDTO {
    private String petName;
    private String photo;
    private String species;
    private String breed;
    private String gender;
    private String color;
    private String microchipId;
    private String allergies;
    private String medicalWarnings;
    private OwnerContact ownerContact;
    private EmergencyVet emergencyVet;
    private List<VaccinationItem> vaccinations;

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class OwnerContact {
        private String name;
        private String phone;
        private String email;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class EmergencyVet {
        private String name;
        private String phone;
        private String organization;
        private String qualification;
        private String specialty;
    }

    @Data @NoArgsConstructor @AllArgsConstructor @Builder
    public static class VaccinationItem {
        private String vaccineName;
        private LocalDate dateAdministered;
        private LocalDate nextDueDate;
    }
}
