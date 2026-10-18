package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Pet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PetRepository extends JpaRepository<Pet, Long> {
    List<Pet> findByOwnerIdAndIsActiveTrue(Long ownerId);
    List<Pet> findByOwnerIdAndIsActiveTrueOrderByCreatedAtDescIdDesc(Long ownerId);
    Page<Pet> findByOwnerIdAndIsActiveTrue(Long ownerId, Pageable pageable);
    long countByOwnerIdAndIsActiveTrue(Long ownerId);
    Page<Pet> findByIsActiveTrue(Pageable pageable);
    Optional<Pet> findByPassportToken(String passportToken);

    @Query("SELECT p FROM Pet p WHERE p.isActive = true AND LOWER(p.name) LIKE LOWER(CONCAT('%',:search,'%'))")
    Page<Pet> searchByName(@Param("search") String search, Pageable pageable);

    @Query("SELECT p FROM Pet p WHERE p.isActive = true AND p.owner.id = :ownerId AND LOWER(p.name) LIKE LOWER(CONCAT('%',:search,'%'))")
    Page<Pet> searchByOwnerAndName(@Param("ownerId") Long ownerId, @Param("search") String search, Pageable pageable);
}
