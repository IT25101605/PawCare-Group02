package com.pawcare.demo.controller;

import com.pawcare.demo.dto.DashboardDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.DashboardService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard & Reports APIs")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<DashboardDTO> getAdminDashboard() {
        return ResponseEntity.ok(dashboardService.getAdminDashboard());
    }

    @GetMapping("/owner")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Map<String, Object>> getOwnerDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(dashboardService.getOwnerDashboard(user.getId()));
    }

    @GetMapping("/vet")
    @PreAuthorize("hasRole('VETERINARIAN')")
    public ResponseEntity<Map<String, Object>> getVetDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(dashboardService.getVetDashboard(user.getId()));
    }

    @GetMapping("/groomer")
    @PreAuthorize("hasRole('GROOMER')")
    public ResponseEntity<Map<String, Object>> getGroomerDashboard(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(dashboardService.getGroomerDashboard(user.getId()));
    }

    @GetMapping("/store")
    @PreAuthorize("hasRole('STORE_MANAGER')")
    public ResponseEntity<Map<String, Object>> getStoreDashboard() {
        return ResponseEntity.ok(dashboardService.getStoreDashboard());
    }

    @GetMapping("/service")
    @PreAuthorize("hasRole('SERVICE_STAFF')")
    public ResponseEntity<Map<String, Object>> getServiceDashboard() {
        return ResponseEntity.ok(dashboardService.getServiceDashboard());
    }

    @GetMapping("/management")
    @PreAuthorize("hasAnyRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getManagementSummary() {
        return ResponseEntity.ok(dashboardService.getManagementSummary());
    }
}
