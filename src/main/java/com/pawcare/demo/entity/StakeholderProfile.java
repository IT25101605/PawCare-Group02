package com.pawcare.demo.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="stakeholder_profiles")
@Getter @Setter @NoArgsConstructor
public class StakeholderProfile {
    @Id private Long userId;
    @MapsId @OneToOne(fetch=FetchType.LAZY) @JoinColumn(name="user_id") private User user;
    @Column(length=150) private String organization;
    @Column(length=100) private String licenseNumber;
    @Column(length=200) private String qualification;
    private Integer experienceYears;
    @Column(length=150) private String specialty;
    @Column(length=100) private String businessRegistration;
    @Column(length=100) private String employeeId;
    @Column(length=100) private String department;
}

