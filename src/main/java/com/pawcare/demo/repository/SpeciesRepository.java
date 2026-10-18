package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Species;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SpeciesRepository extends JpaRepository<Species, Long> {
    java.util.Optional<Species> findByNameIgnoreCase(String name);
}
