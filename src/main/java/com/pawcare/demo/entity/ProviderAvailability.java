package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.DayOfWeek;
import java.time.LocalTime;

@Entity
@Table(name = "provider_availability",
       uniqueConstraints = @UniqueConstraint(name="uk_provider_day", columnNames={"provider_id","day_of_week"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderAvailability {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="provider_id", nullable=false)
    private User provider;

    @Enumerated(EnumType.STRING)
    @Column(name="day_of_week", nullable=false, length=12)
    private DayOfWeek dayOfWeek;

    @Column(name="start_time", nullable=false)
    private LocalTime startTime;

    @Column(name="end_time", nullable=false)
    private LocalTime endTime;

    @Column(name="is_active", nullable=false)
    @Builder.Default
    private Boolean active = true;
}
