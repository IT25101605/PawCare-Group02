package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reviews", uniqueConstraints = {
        @UniqueConstraint(name = "uk_review_service_once", columnNames = {"user_id", "grooming_appointment_id", "review_type"}),
        @UniqueConstraint(name = "uk_review_product_order_once", columnNames = {"user_id", "product_id", "order_id", "review_type"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grooming_appointment_id")
    private GroomingAppointment groomingAppointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "grooming_service_id")
    private GroomingService service;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groomer_id")
    private User groomer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id")
    private Pet pet;

    @Column(nullable = false)
    private Integer rating;

    @Column(columnDefinition = "TEXT")
    private String comment;

    @Enumerated(EnumType.STRING)
    @Column(name = "review_type", nullable = false, length = 20)
    private ReviewType reviewType;

    // Kept for backward compatibility with the existing schema.
    @Column(name = "is_approved")
    @Builder.Default private Boolean isApproved = true;

    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_status", length = 20)
    @Builder.Default private ModerationStatus moderationStatus = ModerationStatus.APPROVED;

    @Column(name = "is_hidden", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default private Boolean hidden = false;

    @Column(name = "is_reported", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default private Boolean reported = false;

    @Column(name = "report_reason", length = 500)
    private String reportReason;

    @Column(name = "reported_at")
    private LocalDateTime reportedAt;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "moderated_at")
    private LocalDateTime moderatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
        if (moderationStatus == null) moderationStatus = ModerationStatus.APPROVED;
        if (hidden == null) hidden = false;
        if (reported == null) reported = false;
        if (isApproved == null) isApproved = true;
    }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum ReviewType { PRODUCT, SERVICE, VET, GROOMING }
    public enum ModerationStatus { PENDING, APPROVED, REJECTED }
}
