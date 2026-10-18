package com.pawcare.demo.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "products")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "sale_price", precision = 10, scale = 2)
    private BigDecimal salePrice;

    @Column(unique = true, length = 50)
    private String sku;

    @Column(name = "stock_quantity", nullable = false)
    @Builder.Default private Integer stockQuantity = 0;

    @Column(name = "reorder_level", nullable = false, columnDefinition = "integer default 0")
    @Builder.Default private Integer reorderLevel = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @Column(length = 500)
    private String image;

    @Column(length = 100)
    private String brand;

    @Column(length = 50)
    private String weight;

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default private Boolean isDeleted = false;

    @Column(name = "is_active")
    @Builder.Default private Boolean isActive = true;

    @Column(name = "is_featured")
    @Builder.Default private Boolean isFeatured = false;

    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() { createdAt = LocalDateTime.now(); updatedAt = LocalDateTime.now(); }

    @PreUpdate
    protected void onUpdate() { updatedAt = LocalDateTime.now(); }

    public BigDecimal getEffectivePrice() {
        return salePrice != null ? salePrice : price;
    }
}
