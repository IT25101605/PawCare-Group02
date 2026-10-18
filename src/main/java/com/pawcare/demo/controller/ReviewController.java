package com.pawcare.demo.controller;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    private final ReviewService service;
    public ReviewController(ReviewService service) { this.service = service; }

    @PostMapping("/service/{appointmentId}")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<ReviewDTO> createService(@PathVariable Long appointmentId, @Valid @RequestBody ReviewRequestDTO request) {
        return ResponseEntity.ok(service.createServiceReview(appointmentId, request));
    }

    @GetMapping("/service/{appointmentId}/mine")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<ReviewDTO> myService(@PathVariable Long appointmentId) {
        var review = service.myServiceReview(appointmentId);
        if (review.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(review.get());
    }

    @PostMapping("/product/{orderId}/{productId}")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<ReviewDTO> createProduct(@PathVariable Long orderId, @PathVariable Long productId,
                                                    @Valid @RequestBody ReviewRequestDTO request) {
        return ResponseEntity.ok(service.createProductReview(orderId, productId, request));
    }

    @GetMapping("/product/{orderId}/{productId}/mine")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<ReviewDTO> myProduct(@PathVariable Long orderId, @PathVariable Long productId) {
        var review = service.myProductReview(orderId, productId);
        if (review.isEmpty()) return ResponseEntity.noContent().build();
        return ResponseEntity.ok(review.get());
    }

    @GetMapping("/product/{productId}")
    public ResponseEntity<ReviewStatsDTO> productStats(@PathVariable Long productId) {
        return ResponseEntity.ok(service.productStats(productId));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Page<ReviewDTO>> mine(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.myReviews(PageRequest.of(page, size)));
    }

    @GetMapping("/store-products")
    @PreAuthorize("hasAnyRole('STORE_MANAGER','ADMIN')")
    public ResponseEntity<Page<ReviewDTO>> storeProductReviews(@RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.productReviewsForStore(PageRequest.of(page, size)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<ReviewDTO> update(@PathVariable Long id, @Valid @RequestBody ReviewRequestDTO request) {
        return ResponseEntity.ok(service.updateOwnReview(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.deleteOwnReview(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ReviewDTO> report(@PathVariable Long id, @Valid @RequestBody ReviewReportRequestDTO request) {
        return ResponseEntity.ok(service.report(id, request));
    }

    @GetMapping("/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Page<ReviewDTO>> moderation(@RequestParam(defaultValue = "ALL") String status,
                                                       @RequestParam(defaultValue = "0") int page,
                                                       @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.moderationQueue(status, PageRequest.of(page, size)));
    }

    @PutMapping("/{id}/moderation")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDTO> moderate(@PathVariable Long id, @Valid @RequestBody com.pawcare.demo.dto.InputRequests.Status body) {
        return ResponseEntity.ok(service.moderate(id, body.status()));
    }

    @PutMapping("/{id}/visibility")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewDTO> visibility(@PathVariable Long id, @RequestParam boolean hidden) {
        return ResponseEntity.ok(service.setHidden(id, hidden));
    }

    @DeleteMapping("/admin/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAsAdmin(@PathVariable Long id) {
        service.deleteAsAdmin(id);
        return ResponseEntity.noContent().build();
    }
}
