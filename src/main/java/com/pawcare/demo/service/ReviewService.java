package com.pawcare.demo.service;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.data.domain.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ReviewService {
    private final ReviewRepository reviews;
    private final GroomingAppointmentRepository groomingAppointments;
    private final OrderRepository orders;
    private final ProductRepository products;
    private final UserRepository users;
    private final NotificationService notifications;

    public ReviewService(ReviewRepository reviews,
                         GroomingAppointmentRepository groomingAppointments,
                         OrderRepository orders,
                         ProductRepository products,
                         UserRepository users,
                         NotificationService notifications) {
        this.reviews = reviews;
        this.groomingAppointments = groomingAppointments;
        this.orders = orders;
        this.products = products;
        this.users = users;
        this.notifications = notifications;
    }

    private CustomUserDetails me() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        if (!(principal instanceof CustomUserDetails user)) throw new AccessDeniedException("Authentication required");
        return user;
    }

    private User currentUser() {
        return users.findById(me().getId()).orElseThrow(() -> new ResourceNotFoundException("User", "id", me().getId()));
    }

    @Transactional
    public ReviewDTO createServiceReview(Long appointmentId, ReviewRequestDTO request) {
        GroomingAppointment appointment = groomingAppointments.findById(appointmentId)
                .orElseThrow(() -> new ResourceNotFoundException("GroomingAppointment", "id", appointmentId));
        Long userId = me().getId();
        if (!appointment.getOwner().getId().equals(userId)) throw new AccessDeniedException("This service appointment belongs to another owner");
        if (appointment.getStatus() != GroomingAppointment.GroomingStatus.COMPLETED) {
            throw new BadRequestException("A service can be reviewed only after the appointment is completed");
        }
        if (reviews.findByUserIdAndGroomingAppointmentIdAndReviewType(userId, appointmentId, Review.ReviewType.SERVICE).isPresent()) {
            throw new BadRequestException("You have already reviewed this completed service appointment");
        }

        Review review = Review.builder()
                .user(currentUser()).reviewType(Review.ReviewType.SERVICE)
                .groomingAppointment(appointment).service(appointment.getService()).groomer(appointment.getGroomer())
                .pet(appointment.getPet()).rating(request.rating()).comment(clean(request.review()))
                .moderationStatus(Review.ModerationStatus.APPROVED).isApproved(true).hidden(false).build();
        return toDTO(reviews.save(review));
    }

    @Transactional
    public ReviewDTO createProductReview(Long orderId, Long productId, ReviewRequestDTO request) {
        Order order = orders.findById(orderId).orElseThrow(() -> new ResourceNotFoundException("Order", "id", orderId));
        Long userId = me().getId();
        if (!order.getUser().getId().equals(userId)) throw new AccessDeniedException("This order belongs to another customer");
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("A product can be reviewed only after its order is delivered");
        }
        boolean purchased = order.getItems().stream().anyMatch(i -> i.getProduct().getId().equals(productId));
        if (!purchased) throw new AccessDeniedException("This product was not purchased in the selected order");
        if (reviews.findByUserIdAndProductIdAndOrderIdAndReviewType(userId, productId, orderId, Review.ReviewType.PRODUCT).isPresent()) {
            throw new BadRequestException("You have already reviewed this product for this order");
        }
        Product product = products.findById(productId).orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));

        Review review = Review.builder()
                .user(currentUser()).reviewType(Review.ReviewType.PRODUCT).product(product).order(order)
                .rating(request.rating()).comment(clean(request.review()))
                .moderationStatus(Review.ModerationStatus.APPROVED).isApproved(true).hidden(false).build();
        return toDTO(reviews.save(review));
    }

    public Optional<ReviewDTO> myServiceReview(Long appointmentId) {
        return reviews.findByUserIdAndGroomingAppointmentIdAndReviewType(me().getId(), appointmentId, Review.ReviewType.SERVICE).map(this::toDTO);
    }

    public Optional<ReviewDTO> myProductReview(Long orderId, Long productId) {
        return reviews.findByUserIdAndProductIdAndOrderIdAndReviewType(me().getId(), productId, orderId, Review.ReviewType.PRODUCT).map(this::toDTO);
    }

    public Page<ReviewDTO> myReviews(Pageable pageable) {
        return reviews.findByUserId(me().getId(), newest(pageable)).map(this::toDTO);
    }

    public Page<ReviewDTO> productReviewsForStore(Pageable pageable) {
        return reviews.findByReviewType(Review.ReviewType.PRODUCT, newest(pageable)).map(this::toDTO);
    }

    @Transactional
    public ReviewDTO updateOwnReview(Long id, ReviewRequestDTO request) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        if (!review.getUser().getId().equals(me().getId())) throw new AccessDeniedException("This review belongs to another user");
        review.setRating(request.rating());
        review.setComment(clean(request.review()));
        review.setModerationStatus(Review.ModerationStatus.APPROVED);
        review.setIsApproved(true);
        // Preserve an Admin hide on edit; the owner cannot bypass moderation by editing text.
        review.setModeratedAt(null);
        return toDTO(reviews.save(review));
    }

    @Transactional
    public void deleteOwnReview(Long id) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        if (!review.getUser().getId().equals(me().getId())) throw new AccessDeniedException("This review belongs to another user");
        reviews.delete(review);
    }

    @Transactional
    public void deleteAsAdmin(Long id) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        Long ownerId = review.getUser().getId();
        String kind = review.getReviewType() == Review.ReviewType.PRODUCT ? "product" : "service";
        reviews.delete(review);
        notifications.create(ownerId, "Review removed",
                "An administrator removed your " + kind + " review.",
                review.getReviewType() == Review.ReviewType.PRODUCT ? Notification.NotificationType.PRODUCT : Notification.NotificationType.GROOMING,
                Notification.Priority.INFO, "/pages/dashboard/owner.html", null, "REVIEW",
                "review-admin-delete-" + id + "-" + System.currentTimeMillis());
    }

    public ReviewStatsDTO productStats(Long productId) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Product", "id", productId);
        Map<Integer, Long> breakdown = new LinkedHashMap<>();
        for (int i = 5; i >= 1; i--) breakdown.put(i, 0L);
        for (Object[] row : reviews.productRatingBreakdown(productId)) {
            breakdown.put(((Number) row[0]).intValue(), ((Number) row[1]).longValue());
        }
        double average = Optional.ofNullable(reviews.averageProductRating(productId)).orElse(0.0);
        average = Math.round(average * 100.0) / 100.0;
        List<ReviewDTO> recent = reviews.publicProductReviews(productId, PageRequest.of(0, 10))
                .stream().map(this::toDTO).toList();
        return ReviewStatsDTO.builder().productId(productId).averageRating(average)
                .totalReviews(reviews.countProductReviews(productId)).ratingBreakdown(breakdown)
                .recentReviews(recent).build();
    }

    public Page<ReviewDTO> moderationQueue(String status, Pageable pageable) {
        Pageable sorted = newest(pageable);
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) return reviews.findAll(sorted).map(this::toDTO);
        if ("REPORTED".equalsIgnoreCase(status)) return reviews.findByReportedTrue(sorted).map(this::toDTO);
        Review.ModerationStatus parsed;
        try { parsed = Review.ModerationStatus.valueOf(status.toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Invalid review moderation status"); }
        return reviews.findByModerationStatus(parsed, sorted).map(this::toDTO);
    }

    @Transactional
    public ReviewDTO moderate(Long id, String status) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        Review.ModerationStatus next;
        try { next = Review.ModerationStatus.valueOf(status.toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Status must be PENDING, APPROVED or REJECTED"); }
        review.setModerationStatus(next);
        // Approval is no longer required for publication. Keep this legacy endpoint
        // compatible by mapping REJECTED to hidden and APPROVED to visible.
        review.setIsApproved(true);
        if (next == Review.ModerationStatus.REJECTED) review.setHidden(true);
        if (next == Review.ModerationStatus.APPROVED) review.setHidden(false);
        review.setReported(false);
        review.setReportReason(null);
        review.setReportedAt(null);
        review.setModeratedAt(LocalDateTime.now());
        Review saved = reviews.save(review);
        notifyModeration(saved);
        return toDTO(saved);
    }

    @Transactional
    public ReviewDTO setHidden(Long id, boolean hidden) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        review.setHidden(hidden);
        if (hidden) {
            review.setReported(false);
            review.setReportReason(null);
            review.setReportedAt(null);
        }
        review.setModeratedAt(LocalDateTime.now());
        return toDTO(reviews.save(review));
    }

    @Transactional
    public ReviewDTO report(Long id, ReviewReportRequestDTO request) {
        Review review = reviews.findById(id).orElseThrow(() -> new ResourceNotFoundException("Review", "id", id));
        if (review.getUser().getId().equals(me().getId())) throw new BadRequestException("You cannot report your own review");
        if (Boolean.TRUE.equals(review.getHidden())) throw new BadRequestException("Only visible reviews can be reported");
        review.setReported(true);
        review.setReportReason(request.reason().strip());
        review.setReportedAt(LocalDateTime.now());
        return toDTO(reviews.save(review));
    }

    private void notifyModeration(Review review) {
        String kind = review.getReviewType() == Review.ReviewType.PRODUCT ? "product" : "service";
        String state = review.getModerationStatus().name().toLowerCase(Locale.ROOT);
        notifications.create(review.getUser().getId(), "Review " + state,
                "Your " + kind + " review is now " + state + ".",
                review.getReviewType() == Review.ReviewType.PRODUCT ? Notification.NotificationType.PRODUCT : Notification.NotificationType.GROOMING,
                Notification.Priority.INFO, "/pages/dashboard/owner.html", review.getId(), "REVIEW",
                "review-moderation-" + review.getId() + "-" + review.getModerationStatus());
    }

    private Pageable newest(Pageable pageable) {
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
    }

    private String clean(String text) {
        String value = text == null ? "" : text.strip();
        if (value.length() < 2 || value.length() > 2000) throw new BadRequestException("Review must be between 2 and 2000 characters");
        return value;
    }


    private String reviewServiceName(Review review) {
        if (review.getReviewType() == Review.ReviewType.SERVICE && review.getGroomingAppointment() != null) {
            GroomingAppointment appointment = review.getGroomingAppointment();
            if (appointment.getSelectedServiceNames() != null && !appointment.getSelectedServiceNames().isBlank()) {
                return appointment.getSelectedServiceNames();
            }
        }
        return review.getService() == null ? null : review.getService().getName();
    }

    public ReviewDTO toDTO(Review r) {
        Review.ModerationStatus status = r.getModerationStatus();
        if (status == null) status = Boolean.TRUE.equals(r.getIsApproved()) ? Review.ModerationStatus.APPROVED : Review.ModerationStatus.PENDING;
        return ReviewDTO.builder()
                .id(r.getId()).reviewType(r.getReviewType().name()).userId(r.getUser().getId()).userName(r.getUser().getFullName())
                .rating(r.getRating()).review(r.getComment()).moderationStatus(status.name()).hidden(Boolean.TRUE.equals(r.getHidden()))
                .reported(Boolean.TRUE.equals(r.getReported())).reportReason(r.getReportReason()).reportedAt(r.getReportedAt())
                .createdAt(r.getCreatedAt()).updatedAt(r.getUpdatedAt())
                .groomingAppointmentId(r.getGroomingAppointment() == null ? null : r.getGroomingAppointment().getId())
                .serviceId(r.getService() == null ? null : r.getService().getId()).serviceName(reviewServiceName(r))
                .groomerId(r.getGroomer() == null ? null : r.getGroomer().getId()).groomerName(r.getGroomer() == null ? null : r.getGroomer().getFullName())
                .petId(r.getPet() == null ? null : r.getPet().getId()).petName(r.getPet() == null ? null : r.getPet().getName())
                .productId(r.getProduct() == null ? null : r.getProduct().getId()).productName(r.getProduct() == null ? null : r.getProduct().getName())
                .orderId(r.getOrder() == null ? null : r.getOrder().getId()).orderNumber(r.getOrder() == null ? null : r.getOrder().getOrderNumber())
                .verifiedPurchase(r.getReviewType() == Review.ReviewType.PRODUCT && r.getOrder() != null).build();
    }
}
