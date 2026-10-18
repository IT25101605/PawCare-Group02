package com.pawcare.demo.controller;

import com.pawcare.demo.dto.AppointmentDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.AppointmentService;
import com.pawcare.demo.service.AuthService;
import com.pawcare.demo.service.SecurityService;
import com.pawcare.demo.exception.BadRequestException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/appointments")
@Tag(name = "Appointments", description = "Veterinary appointment APIs")
public class AppointmentController {

    private final AppointmentService appointmentService;
    private final AuthService authService;

    public AppointmentController(AppointmentService appointmentService, AuthService authService) {
        this.appointmentService = appointmentService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all appointments")
    public ResponseEntity<Page<AppointmentDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(appointmentService.getAllAppointments(PageRequest.of(page, size)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PET_OWNER')")
    @Operation(summary = "Get my appointments")
    public ResponseEntity<Page<AppointmentDTO>> getMyAppointments(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(appointmentService.getByOwner(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/vet")
    @PreAuthorize("hasRole('VETERINARIAN')")
    @Operation(summary = "Get vet's appointments")
    public ResponseEntity<Page<AppointmentDTO>> getVetAppointments(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(appointmentService.getByVet(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get appointment by ID")
    public ResponseEntity<AppointmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(appointmentService.getById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    @Operation(summary = "Create appointment")
    public ResponseEntity<AppointmentDTO> create(@org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestBody AppointmentDTO dto) {
        return ResponseEntity.ok(appointmentService.create(dto));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')")
    @Operation(summary = "Update appointment")
    public ResponseEntity<AppointmentDTO> update(@PathVariable Long id, @Valid @RequestBody AppointmentDTO dto,
                                                   @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        if (SecurityService.hasRole("PET_OWNER") && "CANCELLED".equals(dto.getStatus())) { appointmentService.assertOwnerCanModify(id); requirePassword(password); }
        return ResponseEntity.ok(appointmentService.update(id, dto));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    @Operation(summary = "Delete appointment")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        if (SecurityService.hasRole("PET_OWNER")) { appointmentService.assertOwnerCanModify(id); requirePassword(password); }
        appointmentService.delete(id);
        return ResponseEntity.noContent().build();
    }
    private void requirePassword(String password) {
        if (!authService.confirmPassword(password)) throw new BadRequestException("Current password is incorrect");
    }
}
