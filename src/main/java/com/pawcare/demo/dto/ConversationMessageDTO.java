package com.pawcare.demo.dto;
import lombok.*;
import java.time.LocalDateTime;
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationMessageDTO {
 private Long id,conversationId,petId,appointmentId,senderId,recipientId;
 private String petName,senderName,recipientName,message,attachmentName,attachmentUrl,attachmentType;
 private LocalDateTime createdAt,readAt;
}
