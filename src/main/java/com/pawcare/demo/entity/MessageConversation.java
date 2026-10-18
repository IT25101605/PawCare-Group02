package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "message_conversations", uniqueConstraints = @UniqueConstraint(name = "uk_message_conversation_pair", columnNames = {"pet_id", "owner_id", "veterinarian_id"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MessageConversation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    private Pet pet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "veterinarian_id", nullable = false)
    private User veterinarian;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "latest_appointment_id")
    private Appointment latestAppointment;

    @Column(name = "created_at", updatable = false, nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "latest_message_at", nullable = false)
    private LocalDateTime latestMessageAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) createdAt = now;
        if (latestMessageAt == null) latestMessageAt = now;
    }
}
