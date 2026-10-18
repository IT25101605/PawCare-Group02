package com.pawcare.demo.controller;

import com.pawcare.demo.dto.PaymentDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.PaymentService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payments", description = "Payment APIs")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','SERVICE_STAFF')")
    public ResponseEntity<Page<PaymentDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                    @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paymentService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Page<PaymentDTO>> getMyPayments(@AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paymentService.getByUser(user.getId(), PageRequest.of(page, size)));
    }

    @PostMapping("/process")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','SERVICE_STAFF')")
    public ResponseEntity<PaymentDTO> process(@AuthenticationPrincipal CustomUserDetails user,
                                               @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.Payment body) {
        Long orderId = body.orderId();
        String method = body.paymentMethod();
        return ResponseEntity.ok(paymentService.processPayment(orderId, user.getId(), method, body.cardNumber(), body.expiryMonth(), body.expiryYear(), body.cvc()));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','SERVICE_STAFF')")
    public ResponseEntity<PaymentDTO> updateStatus(@PathVariable Long id,
                                                    @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.Status body) {
        return ResponseEntity.ok(paymentService.updatePaymentStatus(id, body.status()));
    }
}
