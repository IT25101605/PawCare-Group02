package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name="provider_unavailable_dates",
       uniqueConstraints=@UniqueConstraint(name="uk_provider_unavailable_date",columnNames={"provider_id","unavailable_date"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ProviderUnavailableDate {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch=FetchType.LAZY)
    @JoinColumn(name="provider_id",nullable=false)
    private User provider;

    @Column(name="unavailable_date",nullable=false)
    private LocalDate unavailableDate;

    @Column(length=200)
    private String reason;
}
