package com.pawcare.demo.controller;

import com.pawcare.demo.dto.OrderDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.OrderService;
import com.pawcare.demo.store.facade.StoreCheckoutFacade;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name="Orders",description="Order APIs")
public class OrderController {
    private final OrderService orderService;
    private final com.pawcare.demo.service.PaymentReceiptService receipts;
    private final StoreCheckoutFacade checkoutFacade;
    public OrderController(OrderService orderService, StoreCheckoutFacade checkoutFacade, com.pawcare.demo.service.PaymentReceiptService receipts){
        this.orderService=orderService;this.receipts=receipts;
        this.checkoutFacade=checkoutFacade;
    }

    @GetMapping @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','SERVICE_STAFF')")
    public ResponseEntity<Page<OrderDTO>> getAll(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){
        return ResponseEntity.ok(orderService.getAll(PageRequest.of(page,size)));
    }

    @GetMapping("/my") @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Page<OrderDTO>> getMyOrders(@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){
        return ResponseEntity.ok(orderService.getByUser(user.getId(),PageRequest.of(page,size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<OrderDTO> getById(@PathVariable Long id){return ResponseEntity.ok(orderService.getById(id));}

    @GetMapping(value="/{id}/receipt.pdf", produces="application/pdf")
    @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER','SERVICE_STAFF','PET_OWNER')")
    public ResponseEntity<byte[]> receipt(@PathVariable Long id){
        var order = orderService.getById(id);
        return ResponseEntity.ok().header("Content-Disposition", "attachment; filename=payment-slip-"+order.getOrderNumber()+".pdf")
                .body(receipts.generate(id));
    }

    @PostMapping("/checkout") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<OrderDTO> checkout(@AuthenticationPrincipal CustomUserDetails user,
            @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.Checkout body){
        return ResponseEntity.ok(checkoutFacade.checkout(user.getId(), body));
    }

    @GetMapping("/quote") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Map<String,java.math.BigDecimal>> quote(@AuthenticationPrincipal CustomUserDetails user,@RequestParam Long productId,@RequestParam int quantity,
            @RequestParam(required=false)String city,@RequestParam(required=false)String postalCode,@RequestParam(required=false)String paymentMethod){
        return ResponseEntity.ok(checkoutFacade.quote(user.getId(),productId,quantity,city,postalCode,paymentMethod));
    }

    @PutMapping("/{id}/status") @PreAuthorize("hasAnyRole('ADMIN','STORE_MANAGER')")
    public ResponseEntity<OrderDTO> updateStatus(@PathVariable Long id,@jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.Status body){
        return ResponseEntity.ok(orderService.updateStatus(id,body.status()));
    }
}
