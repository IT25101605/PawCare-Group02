package com.pawcare.demo.controller;

import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.ProviderAvailabilityService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/availability")
public class AvailabilityController {
    private final ProviderAvailabilityService service;

    public AvailabilityController(ProviderAvailabilityService service) { this.service = service; }

    public record AvailabilityRequest(@NotBlank String dayOfWeek, @NotNull LocalTime startTime,
                                      @NotNull LocalTime endTime, boolean active) {}

    @GetMapping("/provider/{providerId}")
    public ResponseEntity<List<Map<String,Object>>> provider(@PathVariable Long providerId) {
        return ResponseEntity.ok(service.getForProvider(providerId));
    }

    @PutMapping("/me")
    public ResponseEntity<Map<String,Object>> save(@AuthenticationPrincipal CustomUserDetails user,
                                                    @Valid @RequestBody AvailabilityRequest request) {
        return ResponseEntity.ok(service.saveDay(user.getId(), request.dayOfWeek(), request.startTime(), request.endTime(), request.active()));
    }


    public record UnavailableDateRequest(@NotNull LocalDate date, @Size(max=200) String reason) {}

    @GetMapping("/me/unavailable-dates")
    public ResponseEntity<List<Map<String,Object>>> unavailable(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.getUnavailableDates(user.getId()));
    }

    @PostMapping("/me/unavailable-dates")
    public ResponseEntity<Map<String,Object>> blockDate(@AuthenticationPrincipal CustomUserDetails user,
                                                        @Valid @RequestBody UnavailableDateRequest request) {
        return ResponseEntity.ok(service.addUnavailableDate(user.getId(),request.date(),request.reason()));
    }

    @DeleteMapping("/me/unavailable-dates/{id}")
    public ResponseEntity<Void> unblockDate(@AuthenticationPrincipal CustomUserDetails user,@PathVariable Long id) {
        service.deleteUnavailableDate(user.getId(),id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/slots")
    public ResponseEntity<List<String>> slots(@RequestParam Long providerId,
                                               @RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate date,
                                               @RequestParam String kind,
                                               @RequestParam(required=false) Long excludeId,
                                               @RequestParam(required=false) @Min(15) @Max(480) Integer durationMinutes) {
        return ResponseEntity.ok(service.availableSlots(providerId, date, kind, excludeId, durationMinutes));
    }
}
