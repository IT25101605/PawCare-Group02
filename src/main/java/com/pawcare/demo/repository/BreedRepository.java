package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Breed;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface BreedRepository extends JpaRepository<Breed, Long> {
    List<Breed> findBySpeciesId(Long speciesId);
    java.util.Optional<Breed> findByNameIgnoreCaseAndSpeciesId(String name, Long speciesId);
}
