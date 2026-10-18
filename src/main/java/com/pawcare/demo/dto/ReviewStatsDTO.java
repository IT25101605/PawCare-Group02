package com.pawcare.demo.dto;

import lombok.*;
import java.util.*;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewStatsDTO {
    private Long productId;
    private double averageRating;
    private long totalReviews;
    private Map<Integer, Long> ratingBreakdown;
    private List<ReviewDTO> recentReviews;
}
