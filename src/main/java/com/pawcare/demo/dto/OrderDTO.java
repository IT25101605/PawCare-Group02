package com.pawcare.demo.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderDTO {
    private java.time.LocalDateTime createdAt;
    private Long id;
    private String orderNumber;
    private Long userId;
    private String userName;
    private BigDecimal subtotal;
    private BigDecimal tax;
    private BigDecimal shippingCost;
    private BigDecimal totalAmount;
    private String status;
    private String shippingAddress;
    private String deliveryCity;
    private String postalCode;
    private String checkoutPaymentMethod;
    private String billingAddress;
    private String notes;
    private List<OrderItemDTO> items;
}
