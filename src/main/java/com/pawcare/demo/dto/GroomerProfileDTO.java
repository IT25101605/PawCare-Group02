package com.pawcare.demo.dto;

import lombok.*;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class GroomerProfileDTO {
    private Long id;
    private String name;
    private String profileImage;
    private String organization;
    private String specialty;
    private String qualification;
    private Integer experienceYears;
    private double averageRating;
    private long totalReviews;
    private List<ReviewDTO> recentReviews;
}
