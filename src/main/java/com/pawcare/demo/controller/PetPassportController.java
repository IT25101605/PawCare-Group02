package com.pawcare.demo.controller;

import com.pawcare.demo.dto.PetPassportDTO;
import com.pawcare.demo.dto.PublicPetPassportDTO;
import com.pawcare.demo.service.PetPassportService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class PetPassportController {
    private final PetPassportService service;
    public PetPassportController(PetPassportService service) { this.service = service; }

    @GetMapping("/pets/{petId}/passport")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<PetPassportDTO> settings(@PathVariable Long petId) {
        return ResponseEntity.ok(service.getSettings(petId));
    }

    @PutMapping("/pets/{petId}/passport")
    @PreAuthorize("hasRole('PET_OWNER')")
    public ResponseEntity<PetPassportDTO> update(@PathVariable Long petId, @Valid @RequestBody PetPassportDTO dto) {
        return ResponseEntity.ok(service.updateSettings(petId, dto));
    }

    @GetMapping("/passports/{token}")
    public ResponseEntity<PublicPetPassportDTO> publicPassport(@PathVariable String token) {
        return ResponseEntity.ok(service.getPublic(token));
    }
}
