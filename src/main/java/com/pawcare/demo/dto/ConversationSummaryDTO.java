package com.pawcare.demo.dto;

import lombok.*;
import java.time.LocalDateTime;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationSummaryDTO {
    private Long id;
    private Long petId;
    private String petName;
    private Long otherUserId;
    private String otherUserName;
    private String otherUserRole;
    private Long appointmentId;
    private String appointmentDate;
    private String latestMessage;
    private LocalDateTime latestMessageAt;
    private long unreadCount;
    private String latestAttachmentName;
}
