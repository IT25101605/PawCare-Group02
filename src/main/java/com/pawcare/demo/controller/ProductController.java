package com.pawcare.demo.controller;

import com.pawcare.demo.dto.ProductDTO;
import com.pawcare.demo.entity.ProductCategory;
import com.pawcare.demo.repository.ProductCategoryRepository;
import com.pawcare.demo.service.ProductService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@Tag(name = "Products", description = "Product & Category APIs")
public class ProductController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.pawcare.demo.service.ImageStorageService images;

    private final ProductService productService;
    private final ProductCategoryRepository categoryRepo;
    private final com.pawcare.demo.service.AuthService authService;

    public ProductController(ProductService productService, ProductCategoryRepository categoryRepo, com.pawcare.demo.service.AuthService authService) {
        this.productService = productService;
        this.categoryRepo = categoryRepo;
        this.authService = authService;
    }

    @GetMapping("/categories")
    public ResponseEntity<List<java.util.Map<String,Object>>> getCategories() {
        return ResponseEntity.ok(categoryRepo.findByIsActiveTrue().stream().map(c -> java.util.Map.<String,Object>of("id",c.getId(),"name",c.getName())).toList());
    }

    @PostMapping("/categories")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<java.util.Map<String,Object>> createCategory(@Valid @RequestBody com.pawcare.demo.dto.InputRequests.Category dto) {
        return ResponseEntity.ok(productService.createCategory(dto));
    }

    @GetMapping("/products/manage")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<Page<ProductDTO>> managed(@RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        return ResponseEntity.ok(productService.getManaged(PageRequest.of(page,size)));
    }

    @GetMapping("/products")
    public ResponseEntity<Page<ProductDTO>> getAll(
            @RequestParam(required = false) Long category,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "12") int size) {
        var pageable = PageRequest.of(page, size);
        if (search != null && !search.isBlank()) {
            return ResponseEntity.ok(category!=null?productService.searchCategory(category,search,pageable):productService.search(search, pageable));
        }
        if (category != null) {
            return ResponseEntity.ok(productService.getByCategory(category, pageable));
        }
        return ResponseEntity.ok(productService.getAll(pageable));
    }

    @GetMapping("/products/featured")
    public ResponseEntity<List<ProductDTO>> getFeatured() {
        return ResponseEntity.ok(productService.getFeatured());
    }

    @GetMapping("/products/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(productService.getById(id));
    }

    @PostMapping("/products")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<ProductDTO> create(@org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.create(dto));
    }

    @PostMapping(value="/products",consumes="multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<ProductDTO> createWithImage(
        @org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestPart("product") ProductDTO dto,
        @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image) {
        String path=images.save(image);
        dto.setImage(path);
        try { return ResponseEntity.ok(productService.create(dto)); }
        catch (RuntimeException ex) { images.remove(path); throw ex; }
    }
    @PutMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<ProductDTO> update(@PathVariable Long id, @Valid @RequestBody ProductDTO dto) {
        return ResponseEntity.ok(productService.update(id, dto));
    }

    @PutMapping(value="/products/{id}", consumes="multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<ProductDTO> updateWithImage(@PathVariable Long id, @Valid @RequestPart("product") ProductDTO dto,
        @RequestPart(value="image",required=false) org.springframework.web.multipart.MultipartFile image) {
        String path=images.save(image);
        if (path != null) dto.setImage(path);
        try { return ResponseEntity.ok(productService.update(id,dto)); }
        catch (RuntimeException ex) { images.remove(path); throw ex; }
    }

    @DeleteMapping("/products/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<Void> delete(@PathVariable Long id, @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        if (com.pawcare.demo.service.SecurityService.hasRole("STORE_MANAGER") && !authService.confirmPassword(password)) throw new com.pawcare.demo.exception.BadRequestException("Incorrect password. Deletion was not performed.");
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
