package com.pawcare.demo.dto;

import lombok.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class VeterinarianProfileDTO {
    private Long id;
    private String name;
    private String profileImage;
    private String organization;
    private String qualification;
    private Integer experienceYears;
    private String specialty;
}
