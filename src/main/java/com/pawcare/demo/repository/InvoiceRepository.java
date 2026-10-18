package com.pawcare.demo.repository;

import com.pawcare.demo.entity.Invoice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    java.util.List<Invoice> findByOrderId(Long orderId);
    Page<Invoice> findByUserId(Long userId, Pageable pageable);
    Page<Invoice> findByOrderId(Long orderId, Pageable pageable);
}
