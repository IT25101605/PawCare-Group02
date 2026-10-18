package com.pawcare.demo.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import java.math.BigDecimal;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class ProductDTO {
    private Long id;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Product name is required")
    @Size(min=2,max=100, message="Product name must be between 2 and 100 characters")
    @Pattern(regexp=".*\\S.*",message="Product name cannot be blank")
    private String name;
    @Size(max=2000, message="Description must be 2000 characters or fewer")
    private String description;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Price is required")
    @DecimalMin(value="0.01", message="Price must be greater than 0") @DecimalMax(value="99999999.99", message="Price is too large") @Digits(integer=8,fraction=2, message="Price can have at most 2 decimal places")
    private BigDecimal price;
    @DecimalMin("0.01") @Digits(integer=8,fraction=2)
    private BigDecimal salePrice;
    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class) @Pattern(regexp="[A-Za-z0-9_-]{3,50}",message="SKU must be 3–50 letters, numbers, underscores or hyphens")
    private String sku;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Stock quantity is required")
    @Min(0) @Max(1000000)
    private Integer stockQuantity;
    @Min(0) @Max(1000000)
    private Integer reorderLevel;
    private Boolean clearSalePrice;

    @NotNull(groups=com.pawcare.demo.validation.OnCreate.class, message="Category is required")
    @Positive
    private Long categoryId;
    private String categoryName;

    @Size(max=500) @Pattern(regexp="/(?:uploads/[a-f0-9-]+\\.png|images/products/[a-z0-9-]+\\.(?:svg|png))",message="Upload a product image")
    private String image;
    @Size(max=100)
    private String brand;
    @Size(max=50)
    private String weight;
    private Boolean isActive;
    private Boolean isFeatured;
    private Double averageRating;
    private Long reviewCount;
}
