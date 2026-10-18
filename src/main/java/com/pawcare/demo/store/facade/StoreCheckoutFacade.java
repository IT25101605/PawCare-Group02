package com.pawcare.demo.store.facade;

import com.pawcare.demo.dto.InputRequests;
import com.pawcare.demo.dto.OrderDTO;
import com.pawcare.demo.service.OrderService;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.util.Map;

/** Facade Pattern: one entry point for the two store checkout routes and estimates. */
@Service
public class StoreCheckoutFacade {
    private final OrderService orders;
    public StoreCheckoutFacade(OrderService orders) { this.orders = orders; }

    public OrderDTO checkout(Long ownerId, InputRequests.Checkout request) {
        if (request.productId() != null)
            return orders.buyNow(ownerId, request.shippingAddress(), request.city(), request.postalCode(),
                    request.paymentMethod(), request.productId(), request.quantity());
        return orders.createFromCart(ownerId, request.shippingAddress(), request.city(), request.postalCode(),
                request.paymentMethod());
    }

    public Map<String,BigDecimal> quote(Long ownerId, Long productId, int quantity,
                                        String city, String postalCode, String paymentMethod) {
        return orders.quote(productId, quantity, ownerId, city, postalCode, paymentMethod);
    }
}
