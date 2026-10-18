package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long> {
    long countByIsActiveTrueAndIsDeletedFalse();
    Page<Product> findByIsDeletedFalse(Pageable pageable);
    Page<Product> findByCategoryIdAndIsActiveTrueAndIsDeletedFalse(Long categoryId, Pageable pageable);
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);
    Page<Product> findByIsActiveTrueAndIsDeletedFalse(Pageable pageable);
    List<Product> findByIsFeaturedTrueAndIsActiveTrueAndIsDeletedFalseOrderByCreatedAtDescIdDesc();
    Optional<Product> findBySku(String sku);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Product p WHERE p.id = :id")
    Optional<Product> findForUpdate(@Param("id") Long id);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> searchProducts(@Param("q") String query, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.isActive = true AND p.isDeleted = false AND p.category.id = :catId AND (LOWER(p.name) LIKE LOWER(CONCAT('%',:q,'%')) OR LOWER(p.brand) LIKE LOWER(CONCAT('%',:q,'%')))")
    Page<Product> searchByCategory(@Param("catId") Long categoryId, @Param("q") String query, Pageable pageable);
}
