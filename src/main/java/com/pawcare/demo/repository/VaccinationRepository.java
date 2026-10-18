package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Vaccination;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.time.LocalDate;

public interface VaccinationRepository extends JpaRepository<Vaccination, Long> {
    List<Vaccination> findByPetId(Long petId);
    List<Vaccination> findByAdministeredById(Long vetId);
    List<Vaccination> findByNextDueDateBetween(LocalDate start, LocalDate end);
}
