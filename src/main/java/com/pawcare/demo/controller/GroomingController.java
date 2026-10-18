package com.pawcare.demo.controller;

import com.pawcare.demo.dto.GroomingAppointmentDTO;
import com.pawcare.demo.entity.GroomingService;
import com.pawcare.demo.repository.GroomingServiceRepository;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.GroomingAppointmentService;
import com.pawcare.demo.service.AuthService;
import com.pawcare.demo.service.SecurityService;
import com.pawcare.demo.exception.BadRequestException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/grooming")
@Tag(name = "Grooming", description = "Grooming APIs")
public class GroomingController {

    private final GroomingAppointmentService groomingService;
    private final GroomingServiceRepository serviceRepo;
    private final AuthService authService;
    private final com.pawcare.demo.service.GroomingPlannerService planner;
    private final com.pawcare.demo.repository.GroomingAppointmentRepository appointmentRepo;
    private final com.pawcare.demo.service.ImageStorageService images;

    public GroomingController(GroomingAppointmentService groomingService, GroomingServiceRepository serviceRepo, AuthService authService, com.pawcare.demo.service.GroomingPlannerService planner, com.pawcare.demo.repository.GroomingAppointmentRepository appointmentRepo, com.pawcare.demo.service.ImageStorageService images) {
        this.groomingService = groomingService;
        this.serviceRepo = serviceRepo;
        this.authService = authService;
        this.planner = planner;
        this.appointmentRepo = appointmentRepo;
        this.images = images;
    }

    @PostMapping("/plan")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<com.pawcare.demo.service.GroomingPlannerService.Plan> plan(
            @Valid @RequestBody com.pawcare.demo.dto.GroomingPlanRequest request) {
        return ResponseEntity.ok(planner.recommend(request));
    }

    @GetMapping("/services")
    public ResponseEntity<List<GroomingService>> getServices() {
        return ResponseEntity.ok(serviceRepo.findByIsActiveTrue());
    }

    @GetMapping("/services/manage")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<GroomingService>> manageServices() {
        return ResponseEntity.ok(serviceRepo.findByDeletedFalse(org.springframework.data.domain.Sort.by("name")));
    }

    @PostMapping("/services")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomingService> createService(
            @Valid @RequestBody com.pawcare.demo.dto.GroomingServiceRequest request) {
        GroomingService service = new GroomingService();
        applyServiceDetails(service, request);
        return ResponseEntity.status(201).body(serviceRepo.save(service));
    }

    @PutMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomingService> updateService(@PathVariable Long id,
            @Valid @RequestBody com.pawcare.demo.dto.GroomingServiceRequest request) {
        GroomingService service = serviceRepo.findByIdAndDeletedFalse(id).orElseThrow(() ->
                new com.pawcare.demo.exception.ResourceNotFoundException("Grooming service", "id", id));
        return ResponseEntity.ok(saveServiceWithImage(service, request, null));
    }

    @PostMapping(value="/services", consumes="multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomingService> createServiceWithImage(
            @Valid @RequestPart("service") com.pawcare.demo.dto.GroomingServiceRequest request,
            @RequestPart(value="image", required=false) org.springframework.web.multipart.MultipartFile image) {
        return ResponseEntity.status(201).body(saveServiceWithImage(new GroomingService(), request, image));
    }

    @PutMapping(value="/services/{id}", consumes="multipart/form-data")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GroomingService> updateServiceWithImage(@PathVariable Long id,
            @Valid @RequestPart("service") com.pawcare.demo.dto.GroomingServiceRequest request,
            @RequestPart(value="image", required=false) org.springframework.web.multipart.MultipartFile image) {
        GroomingService service = serviceRepo.findByIdAndDeletedFalse(id).orElseThrow(() ->
                new com.pawcare.demo.exception.ResourceNotFoundException("Grooming service", "id", id));
        return ResponseEntity.ok(saveServiceWithImage(service, request, image));
    }

    private GroomingService saveServiceWithImage(GroomingService service,
            com.pawcare.demo.dto.GroomingServiceRequest request, org.springframework.web.multipart.MultipartFile image) {
        String previous = service.getImage();
        String uploaded = images.save(image);
        if (uploaded != null) service.setImage(uploaded);
        else if (Boolean.TRUE.equals(request.removeImage())) service.setImage(null);
        applyServiceDetails(service, request);
        try { service = serviceRepo.saveAndFlush(service); }
        catch (RuntimeException exception) { images.remove(uploaded); throw exception; }
        if (previous != null && !previous.equals(service.getImage())) images.remove(previous);
        return service;
    }

    @DeleteMapping("/services/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<?> deleteService(@PathVariable Long id) {
        GroomingService service = serviceRepo.findByIdAndDeletedFalse(id).orElseThrow(() ->
                new com.pawcare.demo.exception.ResourceNotFoundException("Grooming service", "id", id));
        // Remove from the catalogue without breaking appointment or review references.
        service.setIsActive(false);
        service.setDeleted(true);
        serviceRepo.saveAndFlush(service);
        return ResponseEntity.noContent().build();
    }

    private void applyServiceDetails(GroomingService service, com.pawcare.demo.dto.GroomingServiceRequest request) {
        service.setName(request.name().trim());
        service.setDescription(request.description() == null ? null : request.description().trim());
        service.setDurationMinutes(request.durationMinutes());
        service.setPrice(request.price());
        service.setIsActive(request.isActive());
    }

    @GetMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','GROOMER')")
    public ResponseEntity<Page<GroomingAppointmentDTO>> getAll(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        if ("GROOMER".equals(user.getRole())) {
            return ResponseEntity.ok(groomingService.getByGroomer(user.getId(), PageRequest.of(page, size)));
        }
        return ResponseEntity.ok(groomingService.getAll(PageRequest.of(page, size)));
    }

    @GetMapping("/appointments/my")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<Page<GroomingAppointmentDTO>> getMyAppointments(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(groomingService.getByOwner(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/appointments/groomer")
    @PreAuthorize("hasRole('GROOMER')")
    public ResponseEntity<Page<GroomingAppointmentDTO>> getGroomerAppointments(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(groomingService.getByGroomer(user.getId(), PageRequest.of(page, size)));
    }

    @GetMapping("/appointments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GROOMER','PET_OWNER')")
    public ResponseEntity<GroomingAppointmentDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(groomingService.getById(id));
    }

    @GetMapping("/appointments/groomer/day")
    @PreAuthorize("hasRole('GROOMER')")
    public ResponseEntity<List<GroomingAppointmentDTO>> getDay(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestParam @org.springframework.format.annotation.DateTimeFormat(iso=org.springframework.format.annotation.DateTimeFormat.ISO.DATE) java.time.LocalDate date) {
        return ResponseEntity.ok(groomingService.getDay(user.getId(),date));
    }

    @PostMapping("/appointments")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<GroomingAppointmentDTO> create(@org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestBody GroomingAppointmentDTO dto) {
        return ResponseEntity.ok(groomingService.create(dto));
    }

    @PutMapping("/appointments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GROOMER','PET_OWNER')")
    public ResponseEntity<GroomingAppointmentDTO> update(@PathVariable Long id, @Valid @RequestBody GroomingAppointmentDTO dto,
                                                          @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        if (SecurityService.hasRole("PET_OWNER") && "CANCELLED".equals(dto.getStatus())) { groomingService.assertOwnerCanModify(id); requirePassword(password); }
        return ResponseEntity.ok(groomingService.update(id, dto));
    }

    @DeleteMapping("/appointments/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        if (SecurityService.hasRole("PET_OWNER")) { groomingService.assertOwnerCanModify(id); requirePassword(password); }
        groomingService.delete(id);
        return ResponseEntity.noContent().build();
    }
    private void requirePassword(String password) {
        if (!authService.confirmPassword(password)) throw new BadRequestException("Current password is incorrect");
    }
}
