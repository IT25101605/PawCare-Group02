package com.pawcare.demo.controller;

import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.CartService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/cart")
@Tag(name = "Cart", description = "Shopping cart APIs")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','STORE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getCart(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(cartService.getCart(user.getId()));
    }

    @PostMapping("/add")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Map<String, Object>> addItem(@AuthenticationPrincipal CustomUserDetails user,
            @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.CartAdd body) {
        Long productId = body.productId();
        int quantity = body.quantity();
        return ResponseEntity.ok(cartService.addItem(user.getId(), productId, quantity));
    }

    @PutMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Map<String, Object>> updateItem(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long itemId, @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.InputRequests.Quantity body) {
        return ResponseEntity.ok(cartService.updateItem(user.getId(), itemId, body.quantity()));
    }

    @DeleteMapping("/items/{itemId}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Map<String, Object>> removeItem(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Long itemId) {
        return ResponseEntity.ok(cartService.removeItem(user.getId(), itemId));
    }

    @DeleteMapping("/clear")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal CustomUserDetails user) {
        cartService.clearCart(user.getId());
        return ResponseEntity.noContent().build();
    }
}
