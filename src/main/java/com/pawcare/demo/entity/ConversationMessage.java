package com.pawcare.demo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="conversation_messages")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ConversationMessage {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="conversation_id") private MessageConversation conversation;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="pet_id",nullable=false) private Pet pet;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="appointment_id") private Appointment appointment;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="sender_id",nullable=false) private User sender;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="recipient_id",nullable=false) private User recipient;
 @Column(nullable=false,columnDefinition="TEXT") private String message;
 @Column(name="attachment_name",length=255) private String attachmentName;
 @Column(name="attachment_url",length=500) private String attachmentUrl;
 @Column(name="attachment_type",length=100) private String attachmentType;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @Column(name="read_at") private LocalDateTime readAt;
 @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}
