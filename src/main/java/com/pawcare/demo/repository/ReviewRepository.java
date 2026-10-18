package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Review;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;
import java.util.*;

public interface ReviewRepository extends JpaRepository<Review, Long> {
    @Query("""
        SELECT COUNT(r) FROM Review r WHERE (r.hidden=false OR r.hidden IS NULL)
        AND ((r.reviewType=com.pawcare.demo.entity.Review.ReviewType.PRODUCT AND r.product IS NOT NULL AND r.order IS NOT NULL)
          OR (r.reviewType=com.pawcare.demo.entity.Review.ReviewType.SERVICE AND r.groomer IS NOT NULL))
        """)
    long countPublicReviews();

    @Query("""
        SELECT AVG(r.rating) FROM Review r WHERE (r.hidden=false OR r.hidden IS NULL)
        AND ((r.reviewType=com.pawcare.demo.entity.Review.ReviewType.PRODUCT AND r.product IS NOT NULL AND r.order IS NOT NULL)
          OR (r.reviewType=com.pawcare.demo.entity.Review.ReviewType.SERVICE AND r.groomer IS NOT NULL))
        """)
    Double averagePublicRating();

    Page<Review> findByProductId(Long productId, Pageable pageable);
    Page<Review> findByUserId(Long userId, Pageable pageable);
    Page<Review> findByIsApprovedTrue(Pageable pageable);
    Page<Review> findByModerationStatus(Review.ModerationStatus status, Pageable pageable);
    Page<Review> findByReportedTrue(Pageable pageable);
    Page<Review> findByReviewType(Review.ReviewType reviewType, Pageable pageable);

    Optional<Review> findByUserIdAndGroomingAppointmentIdAndReviewType(Long userId, Long appointmentId, Review.ReviewType reviewType);
    Optional<Review> findByUserIdAndProductIdAndOrderIdAndReviewType(Long userId, Long productId, Long orderId, Review.ReviewType reviewType);

    @Query("""
        SELECT r FROM Review r WHERE r.product.id=:productId
          AND r.order IS NOT NULL
          AND (r.hidden=false OR r.hidden IS NULL)
        ORDER BY r.createdAt DESC, r.id DESC
        """)
    List<Review> publicProductReviews(@Param("productId") Long productId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating),0) FROM Review r WHERE r.product.id=:productId AND r.order IS NOT NULL AND (r.hidden=false OR r.hidden IS NULL)")
    Double averageProductRating(@Param("productId") Long productId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.product.id=:productId AND r.order IS NOT NULL AND (r.hidden=false OR r.hidden IS NULL)")
    long countProductReviews(@Param("productId") Long productId);

    @Query("SELECT r.rating, COUNT(r) FROM Review r WHERE r.product.id=:productId AND r.order IS NOT NULL AND (r.hidden=false OR r.hidden IS NULL) GROUP BY r.rating")
    List<Object[]> productRatingBreakdown(@Param("productId") Long productId);

    @Query("""
        SELECT r FROM Review r WHERE r.groomer.id=:groomerId
          AND r.reviewType=com.pawcare.demo.entity.Review.ReviewType.SERVICE
          AND (r.hidden=false OR r.hidden IS NULL)
        ORDER BY r.createdAt DESC, r.id DESC
        """)
    List<Review> publicGroomerReviews(@Param("groomerId") Long groomerId, Pageable pageable);

    @Query("SELECT COALESCE(AVG(r.rating),0) FROM Review r WHERE r.groomer.id=:groomerId AND r.reviewType=com.pawcare.demo.entity.Review.ReviewType.SERVICE AND (r.hidden=false OR r.hidden IS NULL)")
    Double averageGroomerRating(@Param("groomerId") Long groomerId);

    @Query("SELECT COUNT(r) FROM Review r WHERE r.groomer.id=:groomerId AND r.reviewType=com.pawcare.demo.entity.Review.ReviewType.SERVICE AND (r.hidden=false OR r.hidden IS NULL)")
    long countGroomerReviews(@Param("groomerId") Long groomerId);
}
