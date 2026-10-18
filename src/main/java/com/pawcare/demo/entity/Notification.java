package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name="idx_notification_user_read", columnList="user_id,is_read"),
        @Index(name="idx_notification_created", columnList="created_at")
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", length = 30)
    @Builder.Default private NotificationType notificationType = NotificationType.INFO;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default private Priority priority = Priority.INFO;

    @Column(name = "is_read")
    @Builder.Default private Boolean isRead = false;

    @Column(length = 500)
    private String link;

    @Column(name="related_entity_id")
    private Long relatedEntityId;

    @Column(name="related_entity_type", length=50)
    private String relatedEntityType;

    @Column(name="dedupe_key", length=180, unique=true)
    private String dedupeKey;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); }

    public enum NotificationType {
        INFO, WARNING, SUCCESS, ERROR, APPOINTMENT, GROOMING, MEDICAL, VACCINATION,
        PET, PET_WEIGHT, ORDER, DELIVERY, PRODUCT, INVENTORY, ACCOUNT, ADMIN, PAYMENT, SYSTEM
    }
    public enum Priority { INFO, SUCCESS, WARNING, URGENT }
}
