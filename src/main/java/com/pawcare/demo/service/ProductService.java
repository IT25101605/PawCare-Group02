package com.pawcare.demo.service;

import com.pawcare.demo.dto.ProductDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository categoryRepository;
    private final ReviewRepository reviewRepository;
    private final CartItemRepository cartItems;
    private final ProductImageService productImages;

    public ProductService(ProductRepository productRepository, ProductCategoryRepository categoryRepository, ReviewRepository reviewRepository, ProductImageService productImages, CartItemRepository cartItems) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.reviewRepository = reviewRepository;
        this.productImages = productImages;this.cartItems = cartItems;
    }

    public Page<ProductDTO> getAll(Pageable pageable) {
        return productRepository.findByIsActiveTrueAndIsDeletedFalse(newest(pageable)).map(this::toDTO);
    }

    public Page<ProductDTO> getManaged(Pageable pageable) {
        return productRepository.findByIsDeletedFalse(newest(pageable)).map(this::toDTO);
    }

    @Transactional
    public java.util.Map<String,Object> createCategory(com.pawcare.demo.dto.InputRequests.Category dto) {
        String name=dto.name().strip();
        if (categoryRepository.existsByNameIgnoreCase(name)) throw new BadRequestException("Category already exists");
        ProductCategory category=categoryRepository.save(ProductCategory.builder().name(name).description(dto.description()).isActive(true).build());
        return java.util.Map.of("id",category.getId(),"name",category.getName());
    }

    public Page<ProductDTO> getByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndIsActiveTrueAndIsDeletedFalse(categoryId, newest(pageable)).map(this::toDTO);
    }

    public Page<ProductDTO> search(String query, Pageable pageable) {
        return productRepository.searchProducts(query, newest(pageable)).map(this::toDTO);
    }

    public Page<ProductDTO> searchCategory(Long id, String query, Pageable pageable) {
        return productRepository.searchByCategory(id,query,newest(pageable)).map(this::toDTO);
    }
    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("createdAt"),Sort.Order.desc("id")));}

    public List<ProductDTO> getFeatured() {
        return productRepository.findByIsFeaturedTrueAndIsActiveTrueAndIsDeletedFalseOrderByCreatedAtDescIdDesc()
                .stream().map(this::toDTO).collect(Collectors.toList());
    }

    public ProductDTO getById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        if (Boolean.TRUE.equals(product.getIsDeleted()))
            throw new ResourceNotFoundException("Product", "id", id);
        return toDTO(product);
    }

    @Transactional
    public ProductDTO create(ProductDTO dto) {
        normalize(dto);
        validatePrices(dto.getPrice(), dto.getSalePrice());
        ensureSku(dto.getSku(), null);
        ProductCategory category = categoryRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", dto.getCategoryId()));

        Product product = Product.builder()
                .name(dto.getName()).description(dto.getDescription())
                .price(dto.getPrice()).salePrice(dto.getSalePrice())
                .sku(dto.getSku()).stockQuantity(dto.getStockQuantity()).reorderLevel(dto.getReorderLevel() == null ? 0 : dto.getReorderLevel())
                .category(category).image(dto.getImage()).brand(dto.getBrand())
                .weight(dto.getWeight()).isActive(dto.getIsActive() == null || dto.getIsActive()).isFeatured(dto.getIsFeatured() != null && dto.getIsFeatured())
                .build();

        return toDTO(productRepository.save(product));
    }

    @Transactional
    public ProductDTO update(Long id, ProductDTO dto) {
        normalize(dto);
        Product product = productRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (Boolean.TRUE.equals(product.getIsDeleted())) throw new ResourceNotFoundException("Product", "id", id);
        if (Boolean.TRUE.equals(dto.getClearSalePrice())) product.setSalePrice(null);
        validatePrices(dto.getPrice() != null ? dto.getPrice() : product.getPrice(), dto.getSalePrice() != null ? dto.getSalePrice() : product.getSalePrice());
        if (dto.getSku() != null) { ensureSku(dto.getSku(), id); product.setSku(dto.getSku()); }
        if (dto.getImage() != null) product.setImage(dto.getImage());
        if (dto.getWeight() != null) product.setWeight(dto.getWeight());
        if (dto.getName() != null) product.setName(dto.getName());
        if (dto.getDescription() != null) product.setDescription(dto.getDescription());
        if (dto.getPrice() != null) product.setPrice(dto.getPrice());
        if (dto.getSalePrice() != null) product.setSalePrice(dto.getSalePrice());
        if (dto.getStockQuantity() != null) product.setStockQuantity(dto.getStockQuantity());
        if (dto.getReorderLevel() != null) product.setReorderLevel(dto.getReorderLevel());
        if (dto.getBrand() != null) product.setBrand(dto.getBrand());
        if (dto.getIsActive() != null) product.setIsActive(dto.getIsActive());
        if (dto.getIsFeatured() != null) product.setIsFeatured(dto.getIsFeatured());

        if (dto.getCategoryId() != null) {
            ProductCategory cat = categoryRepository.findById(dto.getCategoryId()).orElseThrow();
            product.setCategory(cat);
        }

        return toDTO(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id) {
        Product product = productRepository.findForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        if (Boolean.TRUE.equals(product.getIsDeleted())) throw new ResourceNotFoundException("Product", "id", id);
        // Retain order items, reviews and invoices for audit/history instead of hard deleting their FK.
        product.setIsDeleted(true);
        product.setIsActive(false);
        product.setIsFeatured(false);
        productRepository.save(product);
        cartItems.deleteByProductId(id);
    }

    private void normalize(ProductDTO dto) {
        if (dto.getName() != null) dto.setName(dto.getName().strip());
        if (dto.getSku() != null) dto.setSku(dto.getSku().strip());
        if (dto.getBrand() != null) dto.setBrand(dto.getBrand().strip());
        if (dto.getDescription() != null) dto.setDescription(dto.getDescription().strip());
        if (dto.getName() != null && (dto.getName().length() < 2 || dto.getName().length() > 100))
            throw new BadRequestException("Product name must be between 2 and 100 characters");
        if (dto.getStockQuantity() != null && dto.getStockQuantity() < 0)
            throw new BadRequestException("Stock quantity cannot be negative");
    }

    private void validatePrices(java.math.BigDecimal price, java.math.BigDecimal sale) {
        if (sale != null && sale.compareTo(price) > 0) throw new BadRequestException("Sale price cannot be greater than the real price.");
    }
    private void ensureSku(String sku, Long id) {
        productRepository.findBySku(sku).ifPresent(p -> {
            if (!p.getId().equals(id)) throw new BadRequestException("SKU already exists");
        });
    }
    private ProductDTO toDTO(Product p) {
        return ProductDTO.builder()
                .id(p.getId()).name(p.getName()).description(p.getDescription())
                .price(p.getPrice()).salePrice(p.getSalePrice()).sku(p.getSku())
                .stockQuantity(p.getStockQuantity()).reorderLevel(p.getReorderLevel()).categoryId(p.getCategory().getId())
                .categoryName(p.getCategory().getName()).image(productImages.resolve(p))
                .brand(p.getBrand()).weight(p.getWeight())
                .isActive(p.getIsActive()).isFeatured(p.getIsFeatured())
                .averageRating(Math.round(java.util.Optional.ofNullable(reviewRepository.averageProductRating(p.getId())).orElse(0.0) * 100.0) / 100.0)
                .reviewCount(reviewRepository.countProductReviews(p.getId()))
                .build();
    }
}
