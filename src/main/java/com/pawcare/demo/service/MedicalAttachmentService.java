package com.pawcare.demo.service;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.ResourceNotFoundException;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.util.*;
@Service @Transactional(readOnly=true)
public class MedicalAttachmentService {
 private final MedicalAttachmentRepository repo; private final MedicalRecordRepository records; private final UserRepository users; private final MedicalRecordService access; private final DocumentStorageService storage;
 public MedicalAttachmentService(MedicalAttachmentRepository repo,MedicalRecordRepository records,UserRepository users,MedicalRecordService access,DocumentStorageService storage){this.repo=repo;this.records=records;this.users=users;this.access=access;this.storage=storage;}
 public List<Map<String,Object>> list(Long recordId){MedicalRecord r=records.findById(recordId).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",recordId));access.assertCanViewPet(r.getPet().getId());return repo.findByMedicalRecordIdOrderByCreatedAtDescIdDesc(recordId).stream().map(this::dto).toList();}
 @Transactional public Map<String,Object> add(Long recordId,MultipartFile file){access.assertCanModifyRecord(recordId);MedicalRecord r=records.findById(recordId).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",recordId));var principal=(CustomUserDetails)SecurityContextHolder.getContext().getAuthentication().getPrincipal();User u=users.findById(principal.getId()).orElseThrow();var f=storage.save(file);if(f==null)throw new com.pawcare.demo.exception.BadRequestException("Choose an attachment file");MedicalAttachment a=MedicalAttachment.builder().medicalRecord(r).uploadedBy(u).fileName(f.originalName()).fileUrl(f.url()).contentType(f.contentType()).fileSize(f.size()).build();return dto(repo.save(a));}
 public FileData file(Long recordId,Long attachmentId){MedicalRecord r=records.findById(recordId).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",recordId));access.assertCanViewPet(r.getPet().getId());MedicalAttachment a=repo.findById(attachmentId).orElseThrow(()->new ResourceNotFoundException("Attachment","id",attachmentId));if(!a.getMedicalRecord().getId().equals(recordId))throw new org.springframework.security.access.AccessDeniedException("Attachment does not belong to this record");return new FileData(a.getFileName(),a.getContentType(),storage.read(a.getFileUrl()));}
 public record FileData(String name,String contentType,byte[] bytes){}
 private Map<String,Object> dto(MedicalAttachment a){Map<String,Object> m=new LinkedHashMap<>();m.put("id",a.getId());m.put("fileName",a.getFileName());m.put("fileUrl","/api/medical-records/"+a.getMedicalRecord().getId()+"/attachments/"+a.getId()+"/file");m.put("contentType",a.getContentType());m.put("fileSize",a.getFileSize());m.put("createdAt",a.getCreatedAt());return m;}
}
