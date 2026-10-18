package com.pawcare.demo.repository;

import com.pawcare.demo.entity.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import java.util.List;
import java.time.LocalDate;

public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long>, JpaSpecificationExecutor<MedicalRecord> {
    List<MedicalRecord> findByPetIdOrderByRecordDateDescIdDesc(Long petId);
    Page<MedicalRecord> findByPetId(Long petId, Pageable pageable);
    Page<MedicalRecord> findByVeterinarianId(Long vetId, Pageable pageable);
    boolean existsByPetIdAndVeterinarianId(Long petId, Long veterinarianId);
    long countByVeterinarianId(Long vetId);
    List<MedicalRecord> findByFollowUpDateBetween(LocalDate start, LocalDate end);
}
