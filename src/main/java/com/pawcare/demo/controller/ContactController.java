package com.pawcare.demo.controller;
import com.pawcare.demo.dto.InputRequests;
import com.pawcare.demo.entity.ContactMessage;
import com.pawcare.demo.repository.ContactMessageRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
@RestController @RequiredArgsConstructor
@RequestMapping("/api/contact")
public class ContactController {
    private final ContactMessageRepository repository;
    @PostMapping
    public java.util.Map<String,String> submit(@Valid @RequestBody InputRequests.Contact dto) {
        var message=new ContactMessage();
        message.setName(dto.name().strip());message.setEmail(dto.email().strip().toLowerCase(java.util.Locale.ROOT));
        message.setSubject(dto.subject().strip());message.setMessage(dto.message().strip());
        repository.save(message);
        return java.util.Map.of("message","Your message has been received. Our team will get back to you.");
    }
    @GetMapping @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.data.domain.Page<ContactMessage> list(@RequestParam(defaultValue="0") int page) {
        return repository.findAll(org.springframework.data.domain.PageRequest.of(Math.max(0,page),20,org.springframework.data.domain.Sort.by("createdAt").descending()));
    }
}

