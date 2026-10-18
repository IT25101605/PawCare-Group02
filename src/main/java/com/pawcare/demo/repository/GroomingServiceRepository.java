package com.pawcare.demo.repository;

import com.pawcare.demo.entity.GroomingService;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GroomingServiceRepository extends JpaRepository<GroomingService, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT COUNT(s) FROM GroomingService s WHERE s.isActive=true AND s.deleted=false")
    long countByIsActiveTrue();
    @org.springframework.data.jpa.repository.Query("SELECT s FROM GroomingService s WHERE s.isActive=true AND s.deleted=false")
    List<GroomingService> findByIsActiveTrue();
    List<GroomingService> findByDeletedFalse(org.springframework.data.domain.Sort sort);
    java.util.Optional<GroomingService> findByIdAndDeletedFalse(Long id);
}
