package com.pawcare.demo.dto;
import jakarta.validation.constraints.*;
public final class InputRequests {
    public record CartAdd(@NotNull @Positive Long productId, @NotNull @Min(1) @Max(1000000) Integer quantity) {}
    public record Quantity(@NotNull @Min(1) @Max(1000000) Integer quantity) {}
    public record Checkout(@NotBlank @Size(min=5,max=500) String shippingAddress,
        @Size(max=100) String city, @Size(max=20) String postalCode,
        @Pattern(regexp="SIMULATED|CREDIT_CARD|DEBIT_CARD|PAYPAL|BANK_TRANSFER|CASH") String paymentMethod,
        @Positive Long productId, @Min(1) @Max(1000000) Integer quantity) {}
    public record Category(@NotBlank @Size(max=100) String name, @Size(max=255) String description) {}
    public record ChangePassword(@NotBlank @Size(max=72) String currentPassword,
        @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S+$",message="Password needs uppercase, lowercase, a number and a symbol") String newPassword) {}
    public record Payment(@NotNull @Positive Long orderId, @NotBlank @Pattern(regexp="SIMULATED|CREDIT_CARD|DEBIT_CARD|PAYPAL|BANK_TRANSFER|CASH") String paymentMethod,
        @Size(max=25) String cardNumber, @Min(1) @Max(12) Integer expiryMonth,
        @Min(2000) @Max(9999) Integer expiryYear, @Pattern(regexp="[0-9]{3}", message="CVC must contain exactly 3 digits") String cvc) {}
    public record Status(@NotBlank @Size(max=30) String status) {}
    public record Forgot(@NotBlank @Email @Size(max=150) String email) {}
    public record Reset(@NotBlank @Size(max=255) String token,
        @NotBlank @Size(min=8,max=72) @Pattern(regexp="^(?=.*[a-z])(?=.*[A-Z])(?=.*[0-9])(?=.*[^A-Za-z0-9\\s])\\S+$",message="Password needs uppercase, lowercase, a number and a symbol") String password) {}
    public record Contact(@NotBlank @Size(max=100) String name, @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(min=3,max=200) String subject,@NotBlank @Size(min=10,max=2000) String message) {}
}
