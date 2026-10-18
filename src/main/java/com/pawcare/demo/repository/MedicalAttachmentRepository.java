package com.pawcare.demo.repository;
import com.pawcare.demo.entity.MedicalAttachment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface MedicalAttachmentRepository extends JpaRepository<MedicalAttachment,Long>{
 List<MedicalAttachment> findByMedicalRecordIdOrderByCreatedAtDescIdDesc(Long medicalRecordId);
}
