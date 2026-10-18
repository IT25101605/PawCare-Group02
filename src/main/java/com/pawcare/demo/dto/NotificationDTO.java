package com.pawcare.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationDTO {
    private Long id;
    private Long userId;
    private String title;
    private String message;
    private String notificationType;
    private String priority;
    private Boolean isRead;
    private String link;
    private Long relatedEntityId;
    private String relatedEntityType;
    private LocalDateTime createdAt;
}
