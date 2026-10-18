package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Payment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PaymentRepository extends JpaRepository<Payment, Long> {
    @org.springframework.data.jpa.repository.Query("SELECT p.order.id FROM Payment p WHERE p.id = :id")
    java.util.Optional<Long> findOrderIdByPaymentId(@org.springframework.data.repository.query.Param("id") Long id);
    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @org.springframework.data.jpa.repository.Query("SELECT p FROM Payment p WHERE p.id = :id")
    java.util.Optional<Payment> findForUpdate(@org.springframework.data.repository.query.Param("id") Long id);
    java.util.List<Payment> findByOrderId(Long orderId);
    Page<Payment> findByUserId(Long userId, Pageable pageable);
    Page<Payment> findByOrderId(Long orderId, Pageable pageable);
}
