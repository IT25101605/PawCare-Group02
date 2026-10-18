package com.pawcare.demo.controller;

import com.pawcare.demo.dto.UserDTO;
import com.pawcare.demo.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "User management APIs")
public class UserController {

    private final UserService userService;
    private final com.pawcare.demo.service.GroomerProfileService groomerProfiles;

    public UserController(UserService userService, com.pawcare.demo.service.GroomerProfileService groomerProfiles) {
        this.userService = userService;
        this.groomerProfiles = groomerProfiles;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Get all users (Admin only)")
    public ResponseEntity<Page<UserDTO>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search) {
        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<UserDTO> result = search != null && !search.isBlank()
                ? userService.searchUsers(search, pageable)
                : userService.getAllUsers(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')")
    @Operation(summary = "Get users by role")
    public ResponseEntity<Page<UserDTO>> getByRole(@PathVariable String role,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(userService.getUsersByRole(role, PageRequest.of(page, size)));
    }

    @org.springframework.beans.factory.annotation.Autowired
    private com.pawcare.demo.repository.StakeholderProfileRepository profiles;
    @GetMapping("/{id}/professional-profile")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String,Object>> profile(@PathVariable Long id) {
        var p=profiles.findById(id).orElseThrow(() -> new com.pawcare.demo.exception.ResourceNotFoundException("Profile","id",id));
        java.util.Map<String,Object> data=new java.util.LinkedHashMap<>();
        data.put("organization",p.getOrganization()); data.put("licenseNumber",p.getLicenseNumber());
        data.put("qualification",p.getQualification()); data.put("experienceYears",p.getExperienceYears());
        data.put("specialty",p.getSpecialty()); data.put("businessRegistration",p.getBusinessRegistration());
        data.put("employeeId",p.getEmployeeId()); data.put("department",p.getDepartment());
        return ResponseEntity.ok(data);
    }
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN') or @securityService.isCurrentUser(#id)")
    @Operation(summary = "Update user")
    public ResponseEntity<UserDTO> update(@PathVariable Long id, @jakarta.validation.Valid @RequestBody UserDTO dto) {
        return ResponseEntity.ok(userService.updateUser(id, dto));
    }


    @GetMapping("/me/groomer-details")
    @PreAuthorize("hasRole('GROOMER')")
    public ResponseEntity<java.util.Map<String,Object>> myGroomerDetails(@AuthenticationPrincipal CustomUserDetails current) {
        return ResponseEntity.ok(groomerProfiles.getDetails(current.getId()));
    }

    @PutMapping("/me/groomer-details")
    @PreAuthorize("hasRole('GROOMER')")
    @org.springframework.transaction.annotation.Transactional
    public ResponseEntity<java.util.Map<String,Object>> updateMyGroomerDetails(
            @AuthenticationPrincipal CustomUserDetails current,
            @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.GroomerDetailsRequest request) {
        return ResponseEntity.ok(groomerProfiles.updateDetails(current.getId(),request));
    }

    @GetMapping("/{id}/groomer-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String,Object>> groomerDetails(@PathVariable Long id) {
        return ResponseEntity.ok(groomerProfiles.getDetails(id));
    }

    @PutMapping("/{id}/groomer-details")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<java.util.Map<String,Object>> updateGroomerDetails(@PathVariable Long id,
            @jakarta.validation.Valid @RequestBody com.pawcare.demo.dto.GroomerDetailsRequest request) {
        return ResponseEntity.ok(groomerProfiles.updateDetails(id,request));
    }

    @PostMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN','GROOMER')")
    @Operation(summary = "Upload or replace the current user profile photo")
    public ResponseEntity<UserDTO> uploadMyProfileImage(@AuthenticationPrincipal CustomUserDetails user,
                                                         @RequestPart("image") MultipartFile image) {
        return ResponseEntity.ok(userService.updateProfileImage(user.getId(), image));
    }

    @DeleteMapping("/me/profile-image")
    @PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN','GROOMER')")
    @Operation(summary = "Remove the current user profile photo")
    public ResponseEntity<UserDTO> removeMyProfileImage(@AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(userService.removeProfileImage(user.getId()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete user (Admin only)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}
