package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "grooming_appointments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class GroomingAppointment {
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
    @JoinColumn(name = "groomer_id")
    private User groomer;

    /**
     * Legacy/primary service kept so existing databases, reports and reviews continue to work.
     * The complete multi-service selection is stored as a compact snapshot on the appointment
     * itself instead of depending on a join-table write during checkout. This avoids booking
     * failures on older PawCare databases whose grooming join table was never created correctly.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "service_id", nullable = false)
    private GroomingService service;

    /** Comma-separated ordered service ids, for example "1,3,5". */
    @Column(name = "selected_service_ids", length = 1000)
    private String selectedServiceIds;

    /** Human-readable service-name snapshot retained even if the catalogue changes later. */
    @Column(name = "selected_service_names", length = 2000)
    private String selectedServiceNames;

    @Column(name = "appointment_date", nullable = false)
    private LocalDate appointmentDate;

    @Column(name = "appointment_time", nullable = false)
    private LocalTime appointmentTime;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default private GroomingStatus status = GroomingStatus.SCHEDULED;

    @Column(name = "special_instructions", columnDefinition = "TEXT")
    private String specialInstructions;

    @Column(name = "comfort_profile", length = 20)
    private String comfortProfile;

    @Column(name = "preferred_finish", length = 30)
    private String preferredFinish;

    /** Null identifies older visits whose detailed tasks were never recorded. */
    @Column(name = "completed_tasks", length = 250)
    private String completedTasks;

    @Column(name = "products_used", length = 500)
    private String productsUsed;

    @Column(name = "next_visit_date")
    private LocalDate nextVisitDate;

    @Column(name = "total_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalPrice;

    /** Persisted visit length used by availability/overlap checks. */
    @Column(name = "total_duration_minutes")
    private Integer totalDurationMinutes;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public enum GroomingStatus { SCHEDULED, CONFIRMED, IN_PROGRESS, COMPLETED, CANCELLED }
}
