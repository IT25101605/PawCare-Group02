package com.pawcare.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ReviewDTO {
    private Long id;
    private String reviewType;
    private Long userId;
    private String userName;
    private Integer rating;
    private String review;
    private String moderationStatus;
    private Boolean hidden;
    private Boolean reported;
    private String reportReason;
    private LocalDateTime reportedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Long groomingAppointmentId;
    private Long serviceId;
    private String serviceName;
    private Long groomerId;
    private String groomerName;
    private Long petId;
    private String petName;

    private Long productId;
    private String productName;
    private Long orderId;
    private String orderNumber;
    private Boolean verifiedPurchase;
}
