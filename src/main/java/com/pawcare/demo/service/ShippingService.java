package com.pawcare.demo.service;

import org.springframework.stereotype.Service;
import java.math.BigDecimal;

@Service
public class ShippingService {
    public static final BigDecimal KURUNEGALA_SHIPPING = new BigDecimal("350.00");
    public static final BigDecimal STANDARD_SHIPPING = new BigDecimal("500.00");

    public boolean isOnlinePayment(String paymentMethod) {
        return paymentMethod == null || paymentMethod.isBlank() || !"CASH".equalsIgnoreCase(paymentMethod.trim());
    }

    public BigDecimal calculateShipping(String city, String postalCode, String paymentMethod) {
        if (!isOnlinePayment(paymentMethod)) return BigDecimal.ZERO;
        String normalizedCity = city == null ? "" : city.trim();
        String normalizedPostal = postalCode == null ? "" : postalCode.trim();
        if ("Kurunegala".equalsIgnoreCase(normalizedCity) || "60000".equals(normalizedPostal)) {
            return KURUNEGALA_SHIPPING;
        }
        return STANDARD_SHIPPING;
    }
}
