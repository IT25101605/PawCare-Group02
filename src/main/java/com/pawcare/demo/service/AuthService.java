package com.pawcare.demo.service;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.*;
import com.pawcare.demo.registration.factory.StakeholderProfileFactory;
import org.springframework.security.authentication.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class AuthService {

    @org.springframework.beans.factory.annotation.Autowired
    private StakeholderProfileRepository profileRepository;
    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final VerificationTokenRepository tokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider tokenProvider;
    private final NotificationService notifications;

    public AuthService(AuthenticationManager authenticationManager, UserRepository userRepository,
                       RoleRepository roleRepository, VerificationTokenRepository tokenRepository,
                       PasswordEncoder passwordEncoder, JwtTokenProvider tokenProvider,
                       NotificationService notifications) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.tokenRepository = tokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenProvider = tokenProvider;
        this.notifications = notifications;
    }

    public AuthResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail().strip().toLowerCase(java.util.Locale.ROOT), request.getPassword()));
        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = tokenProvider.generateToken(authentication);
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();

        return AuthResponse.builder()
                .token(jwt).type("Bearer")
                .userId(userDetails.getId()).email(userDetails.getEmail())
                .firstName(userDetails.getFirstName()).lastName(userDetails.getLastName())
                .role(userDetails.getRole()).message("Login successful")
                .build();
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        request.setEmail(request.getEmail().strip().toLowerCase(java.util.Locale.ROOT));
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        Role role = roleRepository.findByName(request.getRole())
                .orElseThrow(() -> new BadRequestException("Invalid role: " + request.getRole()));

        User user = User.builder()
                .firstName(request.getFirstName()).lastName(request.getLastName())
                .email(request.getEmail()).password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone()).address(request.getAddress())
                .city(request.getCity()).state(request.getState()).zipCode(request.getZipCode())
                .role(role).enabled("PET_OWNER".equals(role.getName())).emailVerified(false)
                .build();

        user = userRepository.save(user);
        if (!"PET_OWNER".equals(role.getName())) {
            StakeholderProfile profile = StakeholderProfileFactory.create(role.getName(), request, user);
            profileRepository.save(profile);
            for (User admin : userRepository.findByRoleName("ADMIN")) {
                if (Boolean.TRUE.equals(admin.getEnabled())) {
                    notifications.create(admin.getId(), "Stakeholder approval required",
                            "A new " + role.getName().replace('_',' ').toLowerCase(java.util.Locale.ROOT) + " account for " + user.getFullName() + " is waiting for approval.",
                            Notification.NotificationType.ADMIN, Notification.Priority.WARNING,
                            "/pages/dashboard/admin.html", user.getId(), "USER",
                            "approval-request-" + user.getId());
                }
            }
            return AuthResponse.builder().userId(user.getId()).role(role.getName())
                .message("Application submitted. An administrator must approve your professional account before you sign in.").build();
        }

        String jwt = tokenProvider.generateTokenFromEmail(user.getEmail(), user.getId(), role.getName());

        return AuthResponse.builder()
                .token(jwt).type("Bearer")
                .userId(user.getId()).email(user.getEmail())
                .firstName(user.getFirstName()).lastName(user.getLastName())
                .role(role.getName()).message("Registration successful")
                .build();
    }

    @Transactional
    public String forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        String token = UUID.randomUUID().toString();
        VerificationToken vt = VerificationToken.builder()
                .token(token).user(user).tokenType("PASSWORD_RESET")
                .expiryDate(java.time.LocalDateTime.now().plusHours(24)).used(false)
                .build();
        tokenRepository.save(vt);

        return "Password reset link has been sent to your email";
    }

    @Transactional
    public String resetPassword(String token, String newPassword) {
        VerificationToken vt = tokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid token"));

        if (vt.isExpired() || vt.getUsed()) {
            throw new BadRequestException("Token has expired or already been used");
        }

        User user = vt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        vt.setUsed(true);
        tokenRepository.save(vt);
        notifications.create(user.getId(), "Password reset",
                "Your PawCare password was reset successfully.",
                Notification.NotificationType.ACCOUNT, Notification.Priority.SUCCESS,
                null, user.getId(), "USER", "password-reset-" + user.getId() + "-" + System.currentTimeMillis());

        return "Password reset successfully";
    }

    @Transactional
    public void changePassword(InputRequests.ChangePassword request) {
        User user = getCurrentUser();
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword()))
            throw new BadRequestException("Current password is incorrect");
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        userRepository.save(user);
        notifications.create(user.getId(), "Password changed",
                "Your password was changed successfully.",
                Notification.NotificationType.ACCOUNT, Notification.Priority.SUCCESS,
                null, user.getId(), "USER", "password-change-" + user.getId() + "-" + System.currentTimeMillis());
    }

    @Transactional(readOnly=true)
    public boolean confirmPassword(String password) {
        User user = getCurrentUser();
        return password != null && passwordEncoder.matches(password, user.getPassword());
    }

    @Transactional(readOnly=true)
    public User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof CustomUserDetails)) {
            throw new BadRequestException("Not authenticated");
        }
        CustomUserDetails userDetails = (CustomUserDetails) auth.getPrincipal();
        return userRepository.findById(userDetails.getId())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userDetails.getId()));
    }
}
