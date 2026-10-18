package com.pawcare.demo.repository;

import com.pawcare.demo.entity.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    boolean existsByNameIgnoreCase(String name);
    List<ProductCategory> findByIsActiveTrue();
    List<ProductCategory> findByParentIsNull();
}
