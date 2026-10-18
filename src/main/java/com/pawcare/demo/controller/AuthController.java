package com.pawcare.demo.controller;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentication", description = "Auth management APIs")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    @Operation(summary = "Login user")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/register")
    @Operation(summary = "Register new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<Map<String, String>> forgotPassword(@Valid @RequestBody InputRequests.Forgot request) {
        String message = authService.forgotPassword(request.email());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<Map<String, String>> resetPassword(@Valid @RequestBody InputRequests.Reset request) {
        String message = authService.resetPassword(request.token(), request.password());
        return ResponseEntity.ok(Map.of("message", message));
    }

    @PutMapping("/change-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String,String>> changePassword(@Valid @RequestBody InputRequests.ChangePassword request) {
        authService.changePassword(request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/confirm-password")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String,Boolean>> confirmPassword(@RequestBody Map<String,String> request) {
        return ResponseEntity.ok(Map.of("valid", authService.confirmPassword(request.get("password"))));
    }

    @GetMapping("/me")
    @org.springframework.security.access.prepost.PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current authenticated user")
    public ResponseEntity<UserDTO> getCurrentUser() {
        var user = authService.getCurrentUser();
        return ResponseEntity.ok(UserDTO.builder()
                .id(user.getId()).firstName(user.getFirstName()).lastName(user.getLastName())
                .email(user.getEmail()).phone(user.getPhone()).role(user.getRole().getName())
                .enabled(user.getEnabled()).emailVerified(user.getEmailVerified())
                .profileImage(user.getProfileImage())
                .address(user.getAddress()).city(user.getCity()).state(user.getState()).zipCode(user.getZipCode())
                .build());
    }
}
