package com.pawcare.demo.controller;

import com.pawcare.demo.dto.MedicalRecordDTO;
import com.pawcare.demo.security.CustomUserDetails;
import com.pawcare.demo.service.MedicalRecordService;
import com.pawcare.demo.service.MedicalReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.*;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController @RequestMapping("/api/medical-records") @Tag(name="Medical Records")
public class MedicalRecordController {
 private final MedicalRecordService service; private final com.pawcare.demo.service.AuthService authService; private final MedicalReportService reports;
 public MedicalRecordController(MedicalRecordService service,com.pawcare.demo.service.AuthService authService,MedicalReportService reports){this.service=service;this.authService=authService;this.reports=reports;}
 @GetMapping @PreAuthorize("hasRole('ADMIN')") public ResponseEntity<Page<MedicalRecordDTO>> all(@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ResponseEntity.ok(service.getAll(PageRequest.of(page,size)));}
 @GetMapping("/pet/{petId}") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<List<MedicalRecordDTO>> pet(@PathVariable Long petId){return ResponseEntity.ok(service.getListByPet(petId));}
 @GetMapping("/vet") @PreAuthorize("hasRole('VETERINARIAN')") public ResponseEntity<Page<MedicalRecordDTO>> vet(@AuthenticationPrincipal CustomUserDetails user,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ResponseEntity.ok(service.getByVet(user.getId(),PageRequest.of(page,size)));}
 @GetMapping("/search") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<Page<MedicalRecordDTO>> search(@RequestParam(required=false)String q,@RequestParam(required=false)Long petId,@RequestParam(required=false)String owner,@RequestParam(required=false)String veterinarian,@RequestParam(required=false)String diagnosis,@RequestParam(required=false)String treatment,@RequestParam(required=false)String recordType,@RequestParam(required=false)Long appointmentId,@RequestParam(required=false)LocalDate from,@RequestParam(required=false)LocalDate to,@RequestParam(defaultValue="0")int page,@RequestParam(defaultValue="10")int size){return ResponseEntity.ok(service.search(q,petId,owner,veterinarian,diagnosis,treatment,recordType,appointmentId,from,to,PageRequest.of(page,size)));}
 @GetMapping("/pet/{petId}/ai-summary") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<Map<String,Object>> summary(@PathVariable Long petId){return ResponseEntity.ok(service.aiSummary(petId));}
 @GetMapping("/explain") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<Map<String,String>> explain(@RequestParam String term){return ResponseEntity.ok(service.explainTerm(term));}
 @GetMapping(value="/pet/{petId}/report.pdf",produces=MediaType.APPLICATION_PDF_VALUE) @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<byte[]> pdf(@PathVariable Long petId,@RequestParam(defaultValue="inline")String disposition){byte[] pdf=reports.generate(petId);return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,("download".equalsIgnoreCase(disposition)?"attachment":"inline")+"; filename=pet-health-report-"+petId+".pdf").body(pdf);}
 @GetMapping("/{id}") @PreAuthorize("hasAnyRole('ADMIN','PET_OWNER','VETERINARIAN')") public ResponseEntity<MedicalRecordDTO> one(@PathVariable Long id){return ResponseEntity.ok(service.getById(id));}
 @PostMapping @PreAuthorize("hasRole('VETERINARIAN')") public ResponseEntity<MedicalRecordDTO> create(@org.springframework.validation.annotation.Validated(com.pawcare.demo.validation.OnCreate.class) @RequestBody MedicalRecordDTO dto,@AuthenticationPrincipal CustomUserDetails user){return ResponseEntity.ok(service.create(dto,user.getId()));}
 @PutMapping("/{id}") @PreAuthorize("hasRole('VETERINARIAN')") public ResponseEntity<MedicalRecordDTO> update(@PathVariable Long id,@Valid @RequestBody MedicalRecordDTO dto){return ResponseEntity.ok(service.update(id,dto));}
 @DeleteMapping("/{id}") @PreAuthorize("hasAnyRole('VETERINARIAN','ADMIN')") public ResponseEntity<Void> delete(@PathVariable Long id,@RequestHeader(value="X-Confirm-Password",required=false)String password){if(com.pawcare.demo.service.SecurityService.hasRole("VETERINARIAN")&&!authService.confirmPassword(password))throw new com.pawcare.demo.exception.BadRequestException("Incorrect password. Deletion was not performed.");service.delete(id);return ResponseEntity.noContent().build();}
}
