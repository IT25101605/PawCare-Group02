package com.pawcare.demo.service;

import com.pawcare.demo.dto.MedicalRecordDTO;
import com.pawcare.demo.medical.specification.MedicalRecordSpecifications;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.security.CustomUserDetails;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class MedicalRecordService {
    private final MedicalAttachmentRepository attachmentRepository;
    private final MedicalRecordRepository recordRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationService notifications;

    public MedicalRecordService(MedicalAttachmentRepository attachmentRepository, MedicalRecordRepository recordRepository, PetRepository petRepository,
                                UserRepository userRepository, AppointmentRepository appointmentRepository,
                                NotificationService notifications) {
        this.attachmentRepository=attachmentRepository; this.recordRepository=recordRepository; this.petRepository=petRepository; this.userRepository=userRepository;
        this.appointmentRepository=appointmentRepository; this.notifications=notifications;
    }

    private Pageable newest(Pageable p){ return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("recordDate"),Sort.Order.desc("id"))); }
    private Long currentUserId(){
        var a=SecurityContextHolder.getContext().getAuthentication();
        if(a==null || !(a.getPrincipal() instanceof CustomUserDetails u)) throw new org.springframework.security.access.AccessDeniedException("Authentication required");
        return u.getId();
    }

    public void assertCanViewPet(Long petId){
        Pet pet=petRepository.findById(petId).orElseThrow(()->new ResourceNotFoundException("Pet","id",petId));
        if(SecurityService.hasRole("ADMIN")) return;
        Long uid=currentUserId();
        if(SecurityService.hasRole("PET_OWNER")){ if(!pet.getOwner().getId().equals(uid)) throw new org.springframework.security.access.AccessDeniedException("This pet belongs to another owner"); return; }
        if(SecurityService.hasRole("VETERINARIAN")){
            if(appointmentRepository.existsByPetIdAndVeterinarianId(petId,uid)||recordRepository.existsByPetIdAndVeterinarianId(petId,uid)) return;
            throw new org.springframework.security.access.AccessDeniedException("You are not assigned to this pet");
        }
        throw new org.springframework.security.access.AccessDeniedException("Medical records are not available for this role");
    }

    public void assertCanModifyRecord(Long recordId){
        MedicalRecord r=recordRepository.findById(recordId).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",recordId));
        if(!SecurityService.hasRole("ADMIN") && !r.getVeterinarian().getId().equals(currentUserId()))
            throw new org.springframework.security.access.AccessDeniedException("Only the record's veterinarian or administrator may modify it");
    }

    public Page<MedicalRecordDTO> getAll(Pageable pageable){ return recordRepository.findAll(newest(pageable)).map(this::toDTO); }
    public Page<MedicalRecordDTO> getByPet(Long petId,Pageable pageable){ assertCanViewPet(petId); return recordRepository.findByPetId(petId,newest(pageable)).map(this::toDTO); }
    public Page<MedicalRecordDTO> getByVet(Long vetId,Pageable pageable){ return recordRepository.findByVeterinarianId(vetId,newest(pageable)).map(this::toDTO); }
    public List<MedicalRecordDTO> getListByPet(Long petId){ assertCanViewPet(petId); return recordRepository.findByPetIdOrderByRecordDateDescIdDesc(petId).stream().map(this::toDTO).collect(Collectors.toList()); }

    public MedicalRecordDTO getById(Long id){
        MedicalRecord r=recordRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",id));
        assertCanViewPet(r.getPet().getId()); return toDTO(r);
    }

    public Page<MedicalRecordDTO> search(String q,Long petId,String owner,String veterinarian,String diagnosis,String treatment,String recordType,Long appointmentId,LocalDate from,LocalDate to,Pageable pageable){
        Long uid=currentUserId();
        Specification<MedicalRecord> spec = MedicalRecordSpecifications.all();
        if (SecurityService.hasRole("VETERINARIAN")) spec = spec.and(MedicalRecordSpecifications.forVeterinarian(uid));
        if (SecurityService.hasRole("PET_OWNER")) spec = spec.and(MedicalRecordSpecifications.forOwner(uid));
        if (petId != null) spec = spec.and(MedicalRecordSpecifications.forPet(petId));
        if (appointmentId != null) spec = spec.and(MedicalRecordSpecifications.forAppointment(appointmentId));
        if (recordType != null && !recordType.isBlank()) spec = spec.and(MedicalRecordSpecifications.recordType(recordType));
        if (from != null) spec = spec.and(MedicalRecordSpecifications.fromDate(from));
        if (to != null) spec = spec.and(MedicalRecordSpecifications.toDate(to));
        if (owner != null && !owner.isBlank()) spec = spec.and(MedicalRecordSpecifications.ownerName(owner));
        if (veterinarian != null && !veterinarian.isBlank()) spec = spec.and(MedicalRecordSpecifications.veterinarianName(veterinarian));
        if (diagnosis != null && !diagnosis.isBlank()) spec = spec.and(MedicalRecordSpecifications.diagnosis(diagnosis));
        if (treatment != null && !treatment.isBlank()) spec = spec.and(MedicalRecordSpecifications.treatment(treatment));
        if (q != null && !q.isBlank()) spec = spec.and(MedicalRecordSpecifications.keyword(q));
        return recordRepository.findAll(spec,newest(pageable)).map(this::toDTO);
    }

    @Transactional
    public MedicalRecordDTO create(MedicalRecordDTO dto,Long vetId){
        Pet pet=petRepository.findById(dto.getPetId()).orElseThrow(()->new ResourceNotFoundException("Pet","id",dto.getPetId()));
        User vet=userRepository.findById(vetId).orElseThrow(()->new ResourceNotFoundException("Veterinarian","id",vetId));
        MedicalRecord r=MedicalRecord.builder().pet(pet).veterinarian(vet).diagnosis(dto.getDiagnosis()).treatment(dto.getTreatment())
                .prescription(dto.getPrescription()).weightAtVisit(dto.getWeightAtVisit()).temperature(dto.getTemperature())
                .followUpDate(dto.getFollowUpDate()).recordDate(dto.getRecordDate()).recordType(normalizeType(dto.getRecordType())).notes(dto.getNotes()).build();
        if(dto.getAppointmentId()!=null){
            Appointment a=appointmentRepository.findById(dto.getAppointmentId()).orElseThrow(()->new BadRequestException("Appointment not found"));
            if(!a.getPet().getId().equals(pet.getId())||(!SecurityService.hasRole("ADMIN")&&!a.getVeterinarian().getId().equals(vetId))) throw new BadRequestException("Appointment must belong to this pet and veterinarian");
            r.setAppointment(a);
        }
        r=recordRepository.save(r);
        notifications.create(pet.getOwner().getId(),"New medical record","A new medical record was added for "+pet.getName()+".",Notification.NotificationType.MEDICAL,Notification.Priority.INFO,"/pages/dashboard/owner.html#pets",r.getId(),"MEDICAL_RECORD","medical-created-"+r.getId());
        return toDTO(r);
    }

    @Transactional
    public MedicalRecordDTO update(Long id,MedicalRecordDTO dto){
        MedicalRecord r=recordRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("MedicalRecord","id",id)); assertCanModifyRecord(id);
        if(dto.getPetId()!=null&&!dto.getPetId().equals(r.getPet().getId())) throw new BadRequestException("The pet linked to a medical record cannot be changed");
        if(dto.getAppointmentId()!=null&&(r.getAppointment()==null||!dto.getAppointmentId().equals(r.getAppointment().getId()))) throw new BadRequestException("The appointment linked to a medical record cannot be changed");
        if(dto.getRecordDate()!=null)r.setRecordDate(dto.getRecordDate());
        if(Boolean.TRUE.equals(dto.getClearWeightAtVisit()))r.setWeightAtVisit(null); if(Boolean.TRUE.equals(dto.getClearTemperature()))r.setTemperature(null); if(Boolean.TRUE.equals(dto.getClearFollowUpDate()))r.setFollowUpDate(null);
        if(dto.getFollowUpDate()!=null&&!dto.getFollowUpDate().equals(r.getFollowUpDate())&&dto.getFollowUpDate().isBefore(LocalDate.now())) throw new BadRequestException("A new follow-up date cannot be in the past");
        if(dto.getDiagnosis()!=null)r.setDiagnosis(dto.getDiagnosis()); if(dto.getTreatment()!=null)r.setTreatment(dto.getTreatment()); if(dto.getPrescription()!=null)r.setPrescription(dto.getPrescription());
        if(dto.getWeightAtVisit()!=null)r.setWeightAtVisit(dto.getWeightAtVisit()); if(dto.getTemperature()!=null)r.setTemperature(dto.getTemperature()); if(dto.getFollowUpDate()!=null)r.setFollowUpDate(dto.getFollowUpDate());
        if(dto.getRecordType()!=null)r.setRecordType(normalizeType(dto.getRecordType())); if(dto.getNotes()!=null)r.setNotes(dto.getNotes());
        r=recordRepository.save(r);
        notifications.create(r.getPet().getOwner().getId(),"Medical record updated",r.getPet().getName()+"'s medical record was updated.",Notification.NotificationType.MEDICAL,Notification.Priority.INFO,"/pages/dashboard/owner.html#pets",r.getId(),"MEDICAL_RECORD","medical-updated-"+r.getId()+"-"+r.getUpdatedAt());
        return toDTO(r);
    }

    @Transactional public void delete(Long id){
        assertCanModifyRecord(id);
        attachmentRepository.deleteAll(attachmentRepository.findByMedicalRecordIdOrderByCreatedAtDescIdDesc(id));
        attachmentRepository.flush();
        recordRepository.deleteById(id);
    }

    public Map<String,Object> aiSummary(Long petId){
        assertCanViewPet(petId); Pet p=petRepository.findById(petId).orElseThrow(()->new ResourceNotFoundException("Pet","id",petId));
        List<MedicalRecord> rs=recordRepository.findByPetIdOrderByRecordDateDescIdDesc(petId);
        Map<String,Object> out=new LinkedHashMap<>(); out.put("label","AI-generated summary — review the original medical records for verified clinical information."); out.put("petName",p.getName()); out.put("recordCount",rs.size());
        out.put("recent",rs.stream().limit(5).map(r->Map.of("date",r.getRecordDate(),"type",normalizeType(r.getRecordType()),"diagnosis",r.getDiagnosis())).toList());
        out.put("upcomingFollowUps",rs.stream().filter(r->r.getFollowUpDate()!=null&&!r.getFollowUpDate().isBefore(LocalDate.now())).sorted(Comparator.comparing(MedicalRecord::getFollowUpDate)).limit(5).map(r->r.getFollowUpDate()).toList());
        return out;
    }

    public Map<String,String> explainTerm(String term){
        if(term==null||term.isBlank()) throw new BadRequestException("Medical term is required");
        String t=term.trim(); String low=t.toLowerCase(Locale.ROOT); String explanation;
        if(low.contains("otitis externa")) explanation="Inflammation of the outer ear canal.";
        else if(low.contains("dermatitis")) explanation="A general term for inflammation or irritation of the skin.";
        else if(low.contains("gastroenteritis")) explanation="Inflammation affecting the stomach and intestines.";
        else if(low.contains("conjunctivitis")) explanation="Inflammation of the tissue around the eye, often called the conjunctiva.";
        else explanation="This is a clinical term recorded by the veterinarian. Review the original medical record and ask the veterinarian for an interpretation specific to your pet.";
        return Map.of("label","AI Explanation","term",t,"explanation",explanation,"notice","This explanation does not change the veterinarian's diagnosis and is not a diagnosis itself.");
    }

    private String normalizeType(String type){return type==null||type.isBlank()?"GENERAL":type.trim().toUpperCase(Locale.ROOT);}
    private MedicalRecordDTO toDTO(MedicalRecord r){
        Appointment a=r.getAppointment();
        return MedicalRecordDTO.builder().id(r.getId()).petId(r.getPet().getId()).petName(r.getPet().getName()).ownerName(r.getPet().getOwner().getFullName())
                .veterinarianId(r.getVeterinarian().getId()).veterinarianName(r.getVeterinarian().getFullName()).appointmentId(a!=null?a.getId():null)
                .appointmentDate(a!=null?a.getAppointmentDate():null).appointmentTime(a!=null?a.getAppointmentTime():null).appointmentReason(a!=null?a.getReason():null)
                .diagnosis(r.getDiagnosis()).treatment(r.getTreatment()).prescription(r.getPrescription()).weightAtVisit(r.getWeightAtVisit()).temperature(r.getTemperature())
                .followUpDate(r.getFollowUpDate()).recordDate(r.getRecordDate()).recordType(normalizeType(r.getRecordType())).notes(r.getNotes()).createdAt(r.getCreatedAt()).build();
    }
}
