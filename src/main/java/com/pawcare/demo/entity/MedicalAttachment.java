package com.pawcare.demo.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
@Entity @Table(name="medical_attachments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalAttachment {
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="medical_record_id",nullable=false) private MedicalRecord medicalRecord;
 @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="uploaded_by",nullable=false) private User uploadedBy;
 @Column(name="file_name",nullable=false,length=255) private String fileName;
 @Column(name="file_url",nullable=false,length=500) private String fileUrl;
 @Column(name="content_type",nullable=false,length=100) private String contentType;
 @Column(name="file_size",nullable=false) private Long fileSize;
 @Column(name="created_at",updatable=false) private LocalDateTime createdAt;
 @PrePersist void onCreate(){createdAt=LocalDateTime.now();}
}
