package com.pawcare.demo.controller;

import com.pawcare.demo.dto.*;
import com.pawcare.demo.service.SecureMessagingService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@RestController
@RequestMapping("/api/messages")
@PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN')")
public class MessageController {
    private final SecureMessagingService service;
    public MessageController(SecureMessagingService service) { this.service = service; }

    @GetMapping
    public ResponseEntity<List<ConversationMessageDTO>> mine() { return ResponseEntity.ok(service.myMessages()); }

    @GetMapping("/conversations")
    public ResponseEntity<List<ConversationSummaryDTO>> conversations(@RequestParam(required = false) String search) {
        return ResponseEntity.ok(service.conversationList(search));
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<List<ConversationMessageDTO>> conversation(@PathVariable Long conversationId) {
        return ResponseEntity.ok(service.thread(conversationId));
    }

    @PutMapping("/conversations/{conversationId}/read")
    public ResponseEntity<Void> readConversation(@PathVariable Long conversationId) {
        service.markConversationRead(conversationId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/thread")
    public ResponseEntity<List<ConversationMessageDTO>> thread(@RequestParam Long petId, @RequestParam Long otherUserId) {
        return ResponseEntity.ok(service.thread(petId, otherUserId));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN')")
    public ResponseEntity<ConversationMessageDTO> send(@RequestParam Long petId,
                                                       @RequestParam Long recipientId,
                                                       @RequestParam(required = false) Long appointmentId,
                                                       @RequestParam(required = false) String message,
                                                       @RequestPart(required = false) MultipartFile file) {
        return ResponseEntity.ok(service.send(petId, recipientId, appointmentId, message, file));
    }

    @GetMapping("/{id}/attachment")
    public ResponseEntity<byte[]> attachment(@PathVariable Long id) {
        var f = service.attachment(id);
        String type = f.contentType() == null ? MediaType.APPLICATION_OCTET_STREAM_VALUE : f.contentType();
        String name = f.name() == null ? "attachment" : f.name().replace("\"", "");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(type))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + name + "\"")
                .body(f.bytes());
    }

    @PutMapping("/{id}/read")
    public ResponseEntity<Void> read(@PathVariable Long id) {
        service.markRead(id);
        return ResponseEntity.noContent().build();
    }
}
