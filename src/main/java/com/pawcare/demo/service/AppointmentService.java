package com.pawcare.demo.service;

import com.pawcare.demo.dto.AppointmentDTO;
import com.pawcare.demo.appointment.chain.AppointmentBookingValidationChain;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.*;
import java.util.Locale;

@Service
@Transactional(readOnly=true)
public class AppointmentService {
    private final AppointmentRepository appointmentRepository;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final ProviderAvailabilityService availabilityService;
    private final NotificationService notifications;

    public AppointmentService(AppointmentRepository appointmentRepository,PetRepository petRepository,
                              UserRepository userRepository,ProviderAvailabilityService availabilityService,
                              NotificationService notifications){
        this.appointmentRepository=appointmentRepository;this.petRepository=petRepository;this.userRepository=userRepository;
        this.availabilityService=availabilityService;this.notifications=notifications;
    }

    public Page<AppointmentDTO> getAllAppointments(Pageable pageable){return appointmentRepository.findAll(newest(pageable)).map(this::toDTO);}
    public Page<AppointmentDTO> getByOwner(Long ownerId,Pageable pageable){return appointmentRepository.findByOwnerId(ownerId,newest(pageable)).map(this::toDTO);}
    public Page<AppointmentDTO> getByVet(Long vetId,Pageable pageable){return appointmentRepository.findByVeterinarianId(vetId,newest(pageable)).map(this::toDTO);}
    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("appointmentDate"),Sort.Order.desc("appointmentTime"),Sort.Order.desc("id")));}

    public AppointmentDTO getById(Long id){
        Appointment a=appointmentRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Appointment","id",id));
        if(SecurityService.hasRole("PET_OWNER"))SecurityService.requireOwner(a.getOwner().getId());
        if(SecurityService.hasRole("VETERINARIAN")&&!new SecurityService().isCurrentUser(a.getVeterinarian().getId()))
            throw new org.springframework.security.access.AccessDeniedException("This appointment is assigned to another veterinarian");
        return toDTO(a);
    }

    public void assertOwnerCanModify(Long id){
        Appointment a=appointmentRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Appointment","id",id));
        SecurityService.requireOwner(a.getOwner().getId());requireOwnerEditable(a);
    }

    @Transactional
    public AppointmentDTO create(AppointmentDTO dto){
        Pet pet=petRepository.findById(dto.getPetId()).orElseThrow(()->new ResourceNotFoundException("Pet","id",dto.getPetId()));
        User vet=userRepository.findForUpdate(dto.getVeterinarianId()).orElseThrow(()->new ResourceNotFoundException("Veterinarian","id",dto.getVeterinarianId()));
        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new BadRequestException("This pet has been removed");requireActiveVet(vet);
        validateBookable(vet,dto.getAppointmentDate(),dto.getAppointmentTime(),-1L);
        Appointment a=Appointment.builder().pet(pet).owner(pet.getOwner()).veterinarian(vet).appointmentDate(dto.getAppointmentDate())
                .appointmentTime(dto.getAppointmentTime()).reason(dto.getReason()).notes(dto.getNotes()).status(Appointment.AppointmentStatus.SCHEDULED).build();
        a=appointmentRepository.save(a);
        notifications.create(vet.getId(),"New veterinary appointment",
                pet.getOwner().getFullName()+" booked "+pet.getName()+" for "+a.getAppointmentDate()+" at "+a.getAppointmentTime()+".",
                Notification.NotificationType.APPOINTMENT,Notification.Priority.INFO,"/pages/dashboard/vet.html",a.getId(),"APPOINTMENT","appt-new-vet-"+a.getId());
        notifications.create(pet.getOwner().getId(),"Appointment booked",
                "Your appointment with "+vet.getFullName()+" is booked for "+a.getAppointmentDate()+" at "+a.getAppointmentTime()+".",
                Notification.NotificationType.APPOINTMENT,Notification.Priority.SUCCESS,"/pages/dashboard/owner.html#appointments",a.getId(),"APPOINTMENT","appt-new-owner-"+a.getId());
        return toDTO(a);
    }

    @Transactional
    public AppointmentDTO update(Long id,AppointmentDTO dto){
        Appointment a=appointmentRepository.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("Appointment","id",id));
        boolean owner=SecurityService.hasRole("PET_OWNER"), vetRole=SecurityService.hasRole("VETERINARIAN");
        if(owner)SecurityService.requireOwner(a.getOwner().getId());
        if(vetRole&&!new SecurityService().isCurrentUser(a.getVeterinarian().getId()))
            throw new org.springframework.security.access.AccessDeniedException("This appointment is assigned to another veterinarian");
        if (isClosed(a.getStatus())) throw new BadRequestException("Closed appointments cannot be modified. Book a new appointment instead.");
        if (dto.getStatus()!=null) validateStatusTransition(a.getStatus(), dto.getStatus(), owner);
        if(dto.getPetId()!=null&&!dto.getPetId().equals(a.getPet().getId()))throw new BadRequestException("The pet linked to an appointment cannot be changed");
        if(owner){
            requireOwnerEditable(a);
            if(dto.getStatus()!=null&&!"CANCELLED".equals(dto.getStatus())&&!a.getStatus().name().equals(dto.getStatus()))
                throw new BadRequestException("Owners may only cancel an appointment");
            if(dto.getNotes()!=null)throw new BadRequestException("Clinical notes can only be changed by staff");
        }

        User targetVet=a.getVeterinarian();
        if(dto.getVeterinarianId()!=null&&!dto.getVeterinarianId().equals(targetVet.getId())){
            targetVet=userRepository.findForUpdate(dto.getVeterinarianId()).orElseThrow(()->new BadRequestException("Veterinarian not found"));
            requireActiveVet(targetVet);
        }
        LocalDate targetDate=dto.getAppointmentDate()!=null?dto.getAppointmentDate():a.getAppointmentDate();
        LocalTime targetTime=dto.getAppointmentTime()!=null?dto.getAppointmentTime():a.getAppointmentTime();
        boolean scheduleChanged=!targetDate.equals(a.getAppointmentDate())||!targetTime.equals(a.getAppointmentTime())||!targetVet.getId().equals(a.getVeterinarian().getId());
        if(scheduleChanged){
            if(a.getStatus()==Appointment.AppointmentStatus.IN_PROGRESS) throw new BadRequestException("An appointment in progress cannot be rescheduled");
            targetVet=userRepository.findForUpdate(targetVet.getId()).orElseThrow(()->new BadRequestException("Veterinarian not found"));
            validateBookable(targetVet,targetDate,targetTime,a.getId());
        }

        Appointment.AppointmentStatus oldStatus=a.getStatus();
        if(dto.getAppointmentDate()!=null)a.setAppointmentDate(dto.getAppointmentDate());
        if(dto.getAppointmentTime()!=null)a.setAppointmentTime(dto.getAppointmentTime());
        if(dto.getReason()!=null)a.setReason(dto.getReason());
        if(dto.getNotes()!=null)a.setNotes(dto.getNotes());
        if(dto.getStatus()!=null) a.setStatus(Appointment.AppointmentStatus.valueOf(dto.getStatus()));
        a.setVeterinarian(targetVet);
        a=appointmentRepository.save(a);

        if(scheduleChanged){
            notifications.create(a.getVeterinarian().getId(),"Appointment rescheduled",
                    a.getPet().getName()+" is scheduled for "+a.getAppointmentDate()+" at "+a.getAppointmentTime()+".",
                    Notification.NotificationType.APPOINTMENT,Notification.Priority.WARNING,"/pages/dashboard/vet.html",a.getId(),"APPOINTMENT",
                    "appt-rescheduled-vet-"+a.getId()+"-"+a.getUpdatedAt());
            notifications.create(a.getOwner().getId(),"Appointment rescheduled",
                    "Your appointment is now "+a.getAppointmentDate()+" at "+a.getAppointmentTime()+".",
                    Notification.NotificationType.APPOINTMENT,Notification.Priority.WARNING,"/pages/dashboard/owner.html#appointments",a.getId(),"APPOINTMENT",
                    "appt-rescheduled-owner-"+a.getId()+"-"+a.getUpdatedAt());
        }
        if(oldStatus!=a.getStatus()){
            String state=a.getStatus().name().replace('_',' ').toLowerCase(Locale.ROOT);
            notifications.create(a.getOwner().getId(),"Appointment updated","Your appointment for "+a.getPet().getName()+" is now "+state+".",
                    Notification.NotificationType.APPOINTMENT,a.getStatus()==Appointment.AppointmentStatus.CANCELLED?Notification.Priority.WARNING:Notification.Priority.INFO,
                    "/pages/dashboard/owner.html#appointments",a.getId(),"APPOINTMENT","appt-status-owner-"+a.getId()+"-"+a.getStatus());
            if(a.getStatus()==Appointment.AppointmentStatus.CANCELLED){
                notifications.create(a.getVeterinarian().getId(),"Appointment cancelled",
                        a.getOwner().getFullName()+" cancelled the appointment for "+a.getPet().getName()+".",
                        Notification.NotificationType.APPOINTMENT,Notification.Priority.WARNING,"/pages/dashboard/vet.html",a.getId(),"APPOINTMENT",
                        "appt-cancel-vet-"+a.getId());
            }
        }
        return toDTO(a);
    }

    @Transactional
    public void delete(Long id){
        Appointment a=appointmentRepository.findById(id).orElseThrow(()->new ResourceNotFoundException("Appointment","id",id));
        SecurityService.requireOwner(a.getOwner().getId());if(SecurityService.hasRole("PET_OWNER"))requireOwnerEditable(a);
        appointmentRepository.deleteById(id);
    }

    private static boolean isClosed(Appointment.AppointmentStatus s){
        return s==Appointment.AppointmentStatus.COMPLETED || s==Appointment.AppointmentStatus.CANCELLED || s==Appointment.AppointmentStatus.NO_SHOW;
    }
    public static java.util.List<String> allowedStatusTransitions(String raw){
        Appointment.AppointmentStatus status;
        try { status=Appointment.AppointmentStatus.valueOf(raw); } catch(Exception ex) { return java.util.List.of(); }
        return switch(status) {
            case SCHEDULED -> java.util.List.of("CONFIRMED", "CANCELLED", "NO_SHOW");
            case CONFIRMED -> java.util.List.of("IN_PROGRESS", "CANCELLED", "NO_SHOW");
            case IN_PROGRESS -> java.util.List.of("COMPLETED");
            default -> java.util.List.of();
        };
    }
    private void validateStatusTransition(Appointment.AppointmentStatus current, String requested, boolean owner){
        Appointment.AppointmentStatus target;
        try { target=Appointment.AppointmentStatus.valueOf(requested); }
        catch(Exception e){ throw new BadRequestException("Invalid appointment status"); }
        if(current==target)return;
        if(owner && target!=Appointment.AppointmentStatus.CANCELLED) throw new BadRequestException("Only staff can advance appointments");
        if(!allowedStatusTransitions(current.name()).contains(target.name()))
            throw new BadRequestException("Cannot change an appointment from " + current + " to " + target);
    }

    private void validateBookable(User vet,LocalDate date,LocalTime time,Long excludeId){
        new AppointmentBookingValidationChain(availabilityService, appointmentRepository)
                .validate(vet, date, time, excludeId);
    }
    private void requireActiveVet(User vet){
        if(!"VETERINARIAN".equals(vet.getRole().getName())||!Boolean.TRUE.equals(vet.getEnabled()))throw new BadRequestException("Choose an active veterinarian");
    }
    private void requireOwnerEditable(Appointment a){
        if(a.getStatus()!=Appointment.AppointmentStatus.SCHEDULED&&a.getStatus()!=Appointment.AppointmentStatus.CONFIRMED)
            throw new BadRequestException("Only scheduled or confirmed appointments can be edited or cancelled");
    }
    static void validateTime(LocalDate date,LocalTime time){
        if(date==null||time==null||!LocalDateTime.of(date,time).isAfter(LocalDateTime.now()))throw new BadRequestException("Choose an appointment date and time in the future");
    }
    private AppointmentDTO toDTO(Appointment a){
        return AppointmentDTO.builder().id(a.getId()).petId(a.getPet().getId()).petName(a.getPet().getName()).ownerId(a.getOwner().getId()).ownerName(a.getOwner().getFullName())
                .veterinarianId(a.getVeterinarian().getId()).veterinarianName(a.getVeterinarian().getFullName()).appointmentDate(a.getAppointmentDate()).appointmentTime(a.getAppointmentTime())
                .reason(a.getReason()).status(a.getStatus().name()).notes(a.getNotes()).build();
    }
}
