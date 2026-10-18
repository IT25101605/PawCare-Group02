package com.pawcare.demo.controller;

import com.pawcare.demo.dto.PetDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.PetService;
import com.pawcare.demo.service.AuthService;
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

import java.util.List;

@RestController
@RequestMapping("/api/pets")
@Tag(name = "Pets", description = "Pet management APIs")
public class PetController {
    @org.springframework.beans.factory.annotation.Autowired
    private com.pawcare.demo.service.ImageStorageService images;

    private final PetService petService;
    private final AuthService authService;

    public PetController(PetService petService, AuthService authService) {
        this.petService = petService;
        this.authService = authService;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all pets (Admin)")
    public ResponseEntity<Page<PetDTO>> getAll(@RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(petService.getAllPets(PageRequest.of(page, size)));
    }

    @GetMapping("/my")
    @PreAuthorize("hasRole('PET_OWNER')")
    @Operation(summary = "Get current user's pets")
    public ResponseEntity<List<PetDTO>> getMyPets(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(petService.getPetListByOwner(user.getId()));
    }

    @GetMapping("/owner/{ownerId}")
    @PreAuthorize("hasAnyRole('ADMIN','VETERINARIAN','GROOMER')")
    @Operation(summary = "Get pets by owner")
    public ResponseEntity<List<PetDTO>> getByOwner(@PathVariable Long ownerId) {
        return ResponseEntity.ok(petService.getPetListByOwner(ownerId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get pet by ID")
    public ResponseEntity<PetDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    @Operation(summary = "Create a pet")
    public ResponseEntity<PetDTO> create(@org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestBody PetDTO dto,
                                          @AuthenticationPrincipal CustomUserDetails user) {
        Long ownerId = "ADMIN".equals(user.getRole()) && dto.getOwnerId() != null ? dto.getOwnerId() : user.getId();
        return ResponseEntity.ok(petService.createPet(dto, ownerId));
    }

    @PostMapping(consumes="multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<PetDTO> createWithPhoto(
        @org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestPart("pet") PetDTO dto,
        @RequestPart(value="photo",required=false) org.springframework.web.multipart.MultipartFile photo,
        @AuthenticationPrincipal CustomUserDetails user) {
        String path = images.save(photo);
        dto.setPhoto(path);
        try {
            Long owner = "ADMIN".equals(user.getRole()) && dto.getOwnerId()!=null ? dto.getOwnerId() : user.getId();
            return ResponseEntity.ok(petService.createPet(dto, owner));
        } catch (RuntimeException ex) { images.remove(path); throw ex; }
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    @Operation(summary = "Update a pet")
    public ResponseEntity<PetDTO> update(@PathVariable Long id, @Valid @RequestBody PetDTO dto,
                                          @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        petService.assertCanModify(id);
        requirePassword(password);
        return ResponseEntity.ok(petService.updatePet(id, dto));
    }

    @PutMapping(value="/{id}", consumes="multipart/form-data")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    public ResponseEntity<PetDTO> updateWithPhoto(@PathVariable Long id, @Valid @RequestPart("pet") PetDTO dto,
        @RequestPart(value="photo",required=false) org.springframework.web.multipart.MultipartFile photo,
        @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        petService.assertCanModify(id);
        requirePassword(password);
        String path=images.save(photo);
        if (path != null) dto.setPhoto(path);
        try { return ResponseEntity.ok(petService.updatePet(id,dto)); }
        catch (RuntimeException ex) { images.remove(path); throw ex; }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER')")
    @Operation(summary = "Delete a pet")
    public ResponseEntity<Void> delete(@PathVariable Long id,
                                       @RequestHeader(value="X-Confirm-Password", required=false) String password) {
        petService.assertCanModify(id);
        requirePassword(password);
        petService.deletePet(id);
        return ResponseEntity.noContent().build();
    }
    private void requirePassword(String password) {
        if (!authService.confirmPassword(password)) {
            throw new BadRequestException("Current password is incorrect");
        }
    }
}
