package com.pawcare.demo.entity;
import jakarta.persistence.*;
import lombok.*;
@Entity @Table(name="contact_messages")
@Getter @Setter @NoArgsConstructor
public class ContactMessage {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @Column(nullable=false,length=100) private String name;
    @Column(nullable=false,length=150) private String email;
    @Column(nullable=false,length=200) private String subject;
    @Column(nullable=false,length=2000) private String message;
    @Column(nullable=false) private java.time.LocalDateTime createdAt=java.time.LocalDateTime.now();
}

