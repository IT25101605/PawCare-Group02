package com.pawcare.demo.controller;

import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.PawCareAssistantService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
public class AssistantController {
    private final PawCareAssistantService service;
    public AssistantController(PawCareAssistantService service) { this.service = service; }

    public record AskRequest(@NotBlank @Size(max=1000) String question, Long petId) {}

    @PostMapping("/ask")
    public ResponseEntity<Map<String,Object>> ask(@Valid @RequestBody AskRequest request,
                                                  @AuthenticationPrincipal CustomUserDetails user) {
        return ResponseEntity.ok(service.answer(request.question(), user, request.petId()));
    }
}
