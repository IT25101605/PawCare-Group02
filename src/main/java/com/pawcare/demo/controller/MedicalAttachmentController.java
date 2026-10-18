package com.pawcare.demo.controller;
import com.pawcare.demo.service.MedicalAttachmentService;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@RestController @RequestMapping("/api/medical-records/{recordId}/attachments")
public class MedicalAttachmentController {
 private final MedicalAttachmentService service; public MedicalAttachmentController(MedicalAttachmentService service){this.service=service;}
 @GetMapping @PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN','ADMIN')") public ResponseEntity<List<Map<String,Object>>> list(@PathVariable Long recordId){return ResponseEntity.ok(service.list(recordId));}
 @GetMapping("/{attachmentId}/file") @PreAuthorize("hasAnyRole('PET_OWNER','VETERINARIAN','ADMIN')") public ResponseEntity<byte[]> file(@PathVariable Long recordId,@PathVariable Long attachmentId){var f=service.file(recordId,attachmentId);return ResponseEntity.ok().contentType(MediaType.parseMediaType(f.contentType())).header(HttpHeaders.CONTENT_DISPOSITION,"inline; filename=\""+f.name().replace("\"","")+"\"").body(f.bytes());}
 @PostMapping(consumes=MediaType.MULTIPART_FORM_DATA_VALUE) @PreAuthorize("hasRole('VETERINARIAN')") public ResponseEntity<Map<String,Object>> add(@PathVariable Long recordId,@RequestPart("file") MultipartFile file){return ResponseEntity.ok(service.add(recordId,file));}
}
