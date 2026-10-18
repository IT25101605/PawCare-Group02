package com.pawcare.demo.service;

import com.pawcare.demo.dto.GroomingAppointmentDTO;
import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.grooming.state.GroomingStateContext;
import com.pawcare.demo.repository.*;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly=true)
public class GroomingAppointmentService {
    private final GroomingAppointmentRepository groomingRepo;
    private final PetRepository petRepository;
    private final UserRepository userRepository;
    private final GroomingServiceRepository serviceRepo;
    private final ProviderAvailabilityService availabilityService;
    private final NotificationService notifications;
    // Behavioral State Pattern: delegates status changes and owner-edit rules.
    private final GroomingStateContext groomingStates = new GroomingStateContext();

    public GroomingAppointmentService(GroomingAppointmentRepository groomingRepo,PetRepository petRepository,
                                      UserRepository userRepository,GroomingServiceRepository serviceRepo,
                                      ProviderAvailabilityService availabilityService,NotificationService notifications){
        this.groomingRepo=groomingRepo;this.petRepository=petRepository;this.userRepository=userRepository;
        this.serviceRepo=serviceRepo;this.availabilityService=availabilityService;this.notifications=notifications;
    }

    public Page<GroomingAppointmentDTO> getAll(Pageable pageable){return groomingRepo.findAll(newest(pageable)).map(this::toDTO);}
    public Page<GroomingAppointmentDTO> getByOwner(Long ownerId,Pageable pageable){return groomingRepo.findByOwnerId(ownerId,newest(pageable)).map(this::toDTO);}
    public Page<GroomingAppointmentDTO> getByGroomer(Long groomerId,Pageable pageable){return groomingRepo.findByGroomerId(groomerId,newest(pageable)).map(this::toDTO);}
    public List<GroomingAppointmentDTO> getDay(Long groomerId, LocalDate date) {
        return groomingRepo.findByGroomerIdAndAppointmentDateOrderByAppointmentTimeAsc(groomerId,date).stream().map(this::toDTO).toList();
    }
    private Pageable newest(Pageable p){return PageRequest.of(p.getPageNumber(),p.getPageSize(),Sort.by(Sort.Order.desc("appointmentDate"),Sort.Order.desc("appointmentTime"),Sort.Order.desc("id")));}

    public GroomingAppointmentDTO getById(Long id){
        GroomingAppointment g=groomingRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("GroomingAppointment","id",id));
        if(SecurityService.hasRole("PET_OWNER"))SecurityService.requireOwner(g.getOwner().getId());
        if(SecurityService.hasRole("GROOMER")&&(g.getGroomer()==null||!new SecurityService().isCurrentUser(g.getGroomer().getId())))
            throw new org.springframework.security.access.AccessDeniedException("This booking is assigned to another groomer");
        return toDTO(g);
    }

    public void assertOwnerCanModify(Long id){
        GroomingAppointment g=groomingRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("GroomingAppointment","id",id));
        SecurityService.requireOwner(g.getOwner().getId());requireOwnerEditable(g);
    }

    @Transactional
    public GroomingAppointmentDTO create(GroomingAppointmentDTO dto){
        Pet pet=petRepository.findById(dto.getPetId()).orElseThrow(()->new ResourceNotFoundException("Pet","id",dto.getPetId()));
        SecurityService.requireOwner(pet.getOwner().getId());
        if (!Boolean.TRUE.equals(pet.getIsActive())) throw new BadRequestException("This pet has been removed");
        if (SecurityService.hasRole("PET_OWNER") && dto.getNotes() != null)
            throw new BadRequestException("Visit notes can only be written by staff");
        if (dto.getCompletedTasks()!=null || dto.getProductsUsed()!=null || dto.getNextVisitDate()!=null || Boolean.TRUE.equals(dto.getClearNextVisitDate()))
            throw new BadRequestException("Visit progress and care reports are added by the groomer after booking");
        List<GroomingService> services=resolveServices(dto);
        validateFinish(dto.getPreferredFinish(),serviceLabel(services));
        if(dto.getGroomerId()==null)throw new BadRequestException("Choose a groomer");
        User groomer=userRepository.findForUpdate(dto.getGroomerId()).orElseThrow(()->new BadRequestException("Choose an active groomer"));
        requireActiveGroomer(groomer);
        int duration=durationMinutes(services);
        validateBookable(groomer,dto.getAppointmentDate(),dto.getAppointmentTime(),duration,-1L);

        GroomingAppointment g=GroomingAppointment.builder().pet(pet).owner(pet.getOwner())
                .service(services.get(0)).groomer(groomer)
                .appointmentDate(dto.getAppointmentDate()).appointmentTime(dto.getAppointmentTime()).specialInstructions(dto.getSpecialInstructions())
                .status(GroomingAppointment.GroomingStatus.SCHEDULED).comfortProfile(dto.getComfortProfile())
                .preferredFinish(dto.getPreferredFinish()).completedTasks("").notes(dto.getNotes()).build();
        applyServiceSelection(g,services);
        // saveAndFlush deliberately surfaces database problems here, before notifications are created.
        // The appointment no longer requires a many-to-many join-table insert, so older databases can book safely.
        g=groomingRepo.saveAndFlush(g);
        String label=serviceLabel(services);
        notifications.create(groomer.getId(),"New grooming booking",
                pet.getOwner().getFullName()+" booked "+pet.getName()+" for "+label+" on "+g.getAppointmentDate()+" at "+g.getAppointmentTime()+".",
                Notification.NotificationType.GROOMING,Notification.Priority.INFO,"/pages/dashboard/groomer.html",g.getId(),"GROOMING","groom-new-groomer-"+g.getId());
        notifications.create(pet.getOwner().getId(),"Grooming booked",
                label+" with "+groomer.getFullName()+" is booked for "+g.getAppointmentDate()+" at "+g.getAppointmentTime()+".",
                Notification.NotificationType.GROOMING,Notification.Priority.SUCCESS,"/pages/dashboard/owner.html#grooming",g.getId(),"GROOMING","groom-new-owner-"+g.getId());
        return toDTO(g);
    }

    @Transactional
    public GroomingAppointmentDTO update(Long id,GroomingAppointmentDTO dto){
        GroomingAppointment g=groomingRepo.findForUpdate(id).orElseThrow(()->new ResourceNotFoundException("GroomingAppointment","id",id));
        boolean owner=SecurityService.hasRole("PET_OWNER");
        if(owner)SecurityService.requireOwner(g.getOwner().getId());
        if(owner){
            requireOwnerEditable(g);
            if(dto.getStatus()!=null&&!"CANCELLED".equals(dto.getStatus())&&!g.getStatus().name().equals(dto.getStatus()))
                throw new BadRequestException("Owners may only cancel a grooming appointment");
            if(dto.getNotes()!=null)throw new BadRequestException("Staff notes can only be changed by staff");
            if(dto.getCompletedTasks()!=null || dto.getProductsUsed()!=null || dto.getNextVisitDate()!=null || Boolean.TRUE.equals(dto.getClearNextVisitDate()))
                throw new BadRequestException("Visit progress and care reports can only be changed by the groomer");
        }
        if(SecurityService.hasRole("GROOMER")){
            if(g.getGroomer()==null||!new SecurityService().isCurrentUser(g.getGroomer().getId())||
                    (dto.getGroomerId()!=null&&!dto.getGroomerId().equals(g.getGroomer().getId())))
                throw new org.springframework.security.access.AccessDeniedException("This booking belongs to another groomer");
        }
        if(dto.getPetId()!=null&&!dto.getPetId().equals(g.getPet().getId()))throw new BadRequestException("The pet linked to a booking cannot be changed");

        List<GroomingService> services=selectedServices(g);
        List<Long> storedIds=parseServiceIds(g.getSelectedServiceIds());
        if(storedIds.isEmpty() && g.getService()!=null) storedIds=List.of(g.getService().getId());
        List<Long> requestedIds=dto.getServiceIds()!=null?dto.getServiceIds():dto.getServiceId()!=null?List.of(dto.getServiceId()):storedIds;
        boolean servicesChanged=!new HashSet<>(storedIds).equals(new HashSet<>(requestedIds));
        if(servicesChanged) services=resolveServices(dto);
        if(services.isEmpty()) throw new BadRequestException("This booking does not have a valid grooming service");

        User targetGroomer=g.getGroomer();
        Long originalGroomerId=targetGroomer==null?null:targetGroomer.getId();
        if(dto.getGroomerId()!=null&&(targetGroomer==null||!dto.getGroomerId().equals(targetGroomer.getId()))){
            targetGroomer=userRepository.findForUpdate(dto.getGroomerId()).orElseThrow(()->new BadRequestException("Choose an active groomer"));
            requireActiveGroomer(targetGroomer);
        }
        if(targetGroomer==null)throw new BadRequestException("Choose a groomer");
        LocalDate targetDate=dto.getAppointmentDate()!=null?dto.getAppointmentDate():g.getAppointmentDate();
        LocalTime targetTime=dto.getAppointmentTime()!=null?dto.getAppointmentTime():g.getAppointmentTime();
        boolean scheduleChanged=!targetDate.equals(g.getAppointmentDate())||!targetTime.equals(g.getAppointmentTime())||
                !targetGroomer.getId().equals(originalGroomerId)||servicesChanged;
        if(scheduleChanged){
            requireOwnerEditable(g);
            targetGroomer=userRepository.findForUpdate(targetGroomer.getId()).orElseThrow(()->new BadRequestException("Choose an active groomer"));
            requireActiveGroomer(targetGroomer);
            validateBookable(targetGroomer,targetDate,targetTime,servicesChanged?durationMinutes(services):storedDurationMinutes(g),g.getId());
        }

        GroomingAppointment.GroomingStatus oldStatus=g.getStatus();
        if (oldStatus==GroomingAppointment.GroomingStatus.CANCELLED)
            throw new BadRequestException("Cancelled grooming bookings cannot be changed");
        // A completed visit can receive an amended care report, but its schedule, tasks,
        // preferences and completed status can never be reopened or changed.
        if (oldStatus==GroomingAppointment.GroomingStatus.COMPLETED
                && ((dto.getStatus()!=null && !"COMPLETED".equals(dto.getStatus()))
                    || dto.getCompletedTasks()!=null || dto.getSpecialInstructions()!=null
                    || dto.getComfortProfile()!=null || dto.getPreferredFinish()!=null || scheduleChanged))
            throw new BadRequestException("Completed grooming bookings cannot be rescheduled or reopened");
        if (oldStatus==GroomingAppointment.GroomingStatus.SCHEDULED && (dto.getCompletedTasks()!=null || dto.getNotes()!=null
                    || dto.getProductsUsed()!=null || dto.getNextVisitDate()!=null || Boolean.TRUE.equals(dto.getClearNextVisitDate())))
            throw new BadRequestException("Groomer must accept and start the booking before recording visit progress");
        if (oldStatus==GroomingAppointment.GroomingStatus.CONFIRMED && (dto.getCompletedTasks()!=null || dto.getNotes()!=null
                    || dto.getProductsUsed()!=null || dto.getNextVisitDate()!=null || Boolean.TRUE.equals(dto.getClearNextVisitDate())))
            throw new BadRequestException("Start the accepted booking before recording visit progress");
        GroomingAppointment.GroomingStatus nextStatus = dto.getStatus() == null
                ? null : groomingStates.transition(oldStatus, dto.getStatus());
        String finish=dto.getPreferredFinish()!=null?dto.getPreferredFinish():g.getPreferredFinish();
        validateFinish(finish,servicesChanged?serviceLabel(services):serviceLabel(g,services));
        if(dto.getPreferredFinish()!=null) {requireOwnerEditable(g);g.setPreferredFinish(dto.getPreferredFinish());}
        if(dto.getNextVisitDate()!=null && !dto.getNextVisitDate().isAfter(targetDate))
            throw new BadRequestException("The next visit should be after this appointment date");
        if((dto.getNotes()!=null || dto.getProductsUsed()!=null || dto.getNextVisitDate()!=null || Boolean.TRUE.equals(dto.getClearNextVisitDate())) && oldStatus==GroomingAppointment.GroomingStatus.CANCELLED)
            throw new BadRequestException("A cancelled visit cannot receive a care report");
        if("COMPLETED".equals(dto.getStatus()) && oldStatus!=GroomingAppointment.GroomingStatus.COMPLETED) {
            if(dto.getCompletedTasks()!=null) GroomingVisitWorkflow.saveTasks(g,dto.getCompletedTasks());
            GroomingVisitWorkflow.requireComplete(g);
        }
        if(servicesChanged) applyServiceSelection(g,services);
        if(dto.getAppointmentDate()!=null)g.setAppointmentDate(dto.getAppointmentDate());
        if(dto.getAppointmentTime()!=null)g.setAppointmentTime(dto.getAppointmentTime());
        if(nextStatus!=null)g.setStatus(nextStatus);
        if(g.getStatus()==GroomingAppointment.GroomingStatus.IN_PROGRESS && oldStatus!=g.getStatus() && g.getCompletedTasks()!=null)
            g.setCompletedTasks("CHECK_IN");
        if(dto.getCompletedTasks()!=null && !"COMPLETED".equals(dto.getStatus())) GroomingVisitWorkflow.saveTasks(g,dto.getCompletedTasks());
        if(dto.getSpecialInstructions()!=null)g.setSpecialInstructions(dto.getSpecialInstructions());
        if(dto.getComfortProfile()!=null)g.setComfortProfile(dto.getComfortProfile());
        if(dto.getNotes()!=null)g.setNotes(dto.getNotes());
        if(dto.getProductsUsed()!=null)g.setProductsUsed(dto.getProductsUsed().strip());
        if(dto.getNextVisitDate()!=null)g.setNextVisitDate(dto.getNextVisitDate());
        if(Boolean.TRUE.equals(dto.getClearNextVisitDate()))g.setNextVisitDate(null);
        g.setGroomer(targetGroomer);g=groomingRepo.saveAndFlush(g);

        if(scheduleChanged){
            String label=serviceLabel(g,selectedServices(g));
            notifications.create(g.getOwner().getId(),"Grooming appointment updated",
                    label+" with "+g.getGroomer().getFullName()+" is scheduled for "+g.getAppointmentDate()+" at "+g.getAppointmentTime()+".",
                    Notification.NotificationType.GROOMING,Notification.Priority.WARNING,"/pages/dashboard/owner.html#grooming",g.getId(),"GROOMING",
                    "groom-rescheduled-owner-"+g.getId()+"-"+System.nanoTime());
            notifications.create(g.getGroomer().getId(),"Grooming booking updated",
                    g.getPet().getName()+" is scheduled for "+label+" on "+g.getAppointmentDate()+" at "+g.getAppointmentTime()+".",
                    Notification.NotificationType.GROOMING,Notification.Priority.WARNING,"/pages/dashboard/groomer.html",g.getId(),"GROOMING",
                    "groom-rescheduled-groomer-"+g.getId()+"-"+System.nanoTime());
        }
        if(oldStatus!=g.getStatus()){
            String state=g.getStatus().name().replace('_',' ').toLowerCase(Locale.ROOT);
            notifications.create(g.getOwner().getId(),"Grooming appointment updated",
                    "Your grooming appointment for "+g.getPet().getName()+" is now "+state+".",
                    Notification.NotificationType.GROOMING,g.getStatus()==GroomingAppointment.GroomingStatus.CANCELLED?Notification.Priority.WARNING:Notification.Priority.INFO,
                    "/pages/dashboard/owner.html#grooming",g.getId(),"GROOMING","groom-status-owner-"+g.getId()+"-"+g.getStatus());
            if(g.getStatus()==GroomingAppointment.GroomingStatus.CANCELLED && g.getGroomer()!=null){
                notifications.create(g.getGroomer().getId(),"Grooming appointment cancelled",
                        g.getOwner().getFullName()+" cancelled the booking for "+g.getPet().getName()+".",
                        Notification.NotificationType.GROOMING,Notification.Priority.WARNING,"/pages/dashboard/groomer.html",g.getId(),"GROOMING",
                        "groom-cancel-groomer-"+g.getId());
            }
        }
        return toDTO(g);
    }

    @Transactional
    public void delete(Long id){
        GroomingAppointment g=groomingRepo.findById(id).orElseThrow(()->new ResourceNotFoundException("GroomingAppointment","id",id));
        if(SecurityService.hasRole("PET_OWNER")){
            SecurityService.requireOwner(g.getOwner().getId());
            requireOwnerEditable(g);
        }
        groomingRepo.deleteById(id);
    }

    private void validateBookable(User groomer,LocalDate date,LocalTime time,int durationMinutes,Long excludeId){
        AppointmentService.validateTime(date,time);
        if(durationMinutes<15 || durationMinutes>480) throw new BadRequestException("The selected grooming services have an invalid total duration");
        if(!availabilityService.isAvailable(groomer,date,time,durationMinutes))
            throw new BadRequestException("The selected services do not fit within the groomer's available hours");
        LocalTime requestedEnd=time.plusMinutes(durationMinutes);
        for(GroomingAppointment other:groomingRepo.findActiveForGroomerDate(groomer.getId(),date)){
            if(excludeId!=null&&excludeId>0&&other.getId().equals(excludeId))continue;
            LocalTime otherStart=other.getAppointmentTime();
            if(otherStart==null)continue;
            LocalTime otherEnd=otherStart.plusMinutes(storedDurationMinutes(other));
            if(time.isBefore(otherEnd)&&otherStart.isBefore(requestedEnd))
                throw new BadRequestException("The selected grooming time overlaps another booking");
        }
    }

    private List<GroomingService> resolveServices(GroomingAppointmentDTO dto){
        List<Long> requested=new ArrayList<>();
        if(dto.getServiceIds()!=null) requested.addAll(dto.getServiceIds());
        else if(dto.getServiceId()!=null) requested.add(dto.getServiceId());
        requested=requested.stream().filter(Objects::nonNull).distinct().toList();
        if(requested.isEmpty())throw new BadRequestException("Choose at least one grooming service");
        if(requested.size()>8)throw new BadRequestException("Choose at most 8 grooming services");
        List<GroomingService> result=new ArrayList<>();
        for(Long serviceId:requested){
            GroomingService svc=serviceRepo.findById(serviceId).orElseThrow(()->new ResourceNotFoundException("GroomingService","id",serviceId));
            if(svc.isDeleted() || !Boolean.TRUE.equals(svc.getIsActive()))throw new BadRequestException(svc.getName()+" is currently unavailable");
            result.add(svc);
        }
        return result;
    }

    /**
     * Reads the scalar multi-service snapshot. Older single-service rows automatically fall back
     * to service_id, so this works without requiring the legacy join table to exist.
     */
    private List<GroomingService> selectedServices(GroomingAppointment g){
        List<Long> ids=parseServiceIds(g.getSelectedServiceIds());
        if(ids.isEmpty()&&g.getService()!=null) ids=List.of(g.getService().getId());
        if(ids.isEmpty()) return List.of();
        Map<Long,GroomingService> byId=serviceRepo.findAllById(ids).stream()
                .collect(Collectors.toMap(GroomingService::getId,Function.identity(),(a,b)->a,LinkedHashMap::new));
        List<GroomingService> ordered=new ArrayList<>();
        for(Long id:ids){GroomingService svc=byId.get(id);if(svc!=null)ordered.add(svc);}
        if(ordered.isEmpty()&&g.getService()!=null) ordered.add(g.getService());
        return ordered;
    }

    private List<Long> parseServiceIds(String csv){
        if(csv==null||csv.isBlank())return new ArrayList<>();
        List<Long> ids=new ArrayList<>();
        for(String part:csv.split(",")){
            try{long id=Long.parseLong(part.trim());if(id>0&&!ids.contains(id))ids.add(id);}catch(NumberFormatException ignored){}
        }
        return ids;
    }

    private void applyServiceSelection(GroomingAppointment g,List<GroomingService> services){
        g.setService(services.get(0));
        g.setSelectedServiceIds(services.stream().map(s->String.valueOf(s.getId())).collect(Collectors.joining(",")));
        g.setSelectedServiceNames(serviceLabel(services));
        g.setTotalPrice(totalPrice(services));
        g.setTotalDurationMinutes(durationMinutes(services));
    }

    private BigDecimal totalPrice(List<GroomingService> services){return services.stream().map(GroomingService::getPrice).filter(Objects::nonNull).reduce(BigDecimal.ZERO,BigDecimal::add);}
    private int durationMinutes(List<GroomingService> services){return services.stream().map(GroomingService::getDurationMinutes).filter(Objects::nonNull).mapToInt(Integer::intValue).sum();}
    private int storedDurationMinutes(GroomingAppointment appointment){
        Integer stored=appointment.getTotalDurationMinutes();
        if(stored!=null&&stored>0)return Math.max(15,stored);
        return appointment.getService()!=null&&appointment.getService().getDurationMinutes()!=null
                ? Math.max(15,appointment.getService().getDurationMinutes()) : 30;
    }
    private String serviceLabel(List<GroomingService> services){return services.stream().map(GroomingService::getName).filter(Objects::nonNull).collect(Collectors.joining(" + "));}
    private String serviceLabel(GroomingAppointment g,List<GroomingService> services){
        return g.getSelectedServiceNames()!=null&&!g.getSelectedServiceNames().isBlank()?g.getSelectedServiceNames():serviceLabel(services);
    }
    private void requireActiveGroomer(User g){if(!"GROOMER".equals(g.getRole().getName())||!Boolean.TRUE.equals(g.getEnabled()))throw new BadRequestException("Choose an active groomer");}
    private void validateFinish(String finish, String label) {
        if(finish==null || "AS_DISCUSSED".equals(finish) || "KEEP_LENGTH".equals(finish))return;
        if(!GroomingVisitWorkflow.supportsStyle(label))
            throw new BadRequestException("Choose a trimming service for this finish, or select keep coat length");
    }
    private void requireOwnerEditable(GroomingAppointment g){
        groomingStates.requireOwnerEditable(g.getStatus());
    }
    private GroomingAppointmentDTO toDTO(GroomingAppointment g){
        List<GroomingService> services=selectedServices(g);
        List<Long> ids=parseServiceIds(g.getSelectedServiceIds());
        if(ids.isEmpty()&&g.getService()!=null)ids=List.of(g.getService().getId());
        String label=serviceLabel(g,services);
        List<String> names=label==null||label.isBlank()?List.of():Arrays.stream(label.split("\\s*\\+\\s*")).filter(s->!s.isBlank()).toList();
        GroomingService primary=g.getService()!=null?g.getService():(services.isEmpty()?null:services.get(0));
        int duration=g.getTotalDurationMinutes()!=null&&g.getTotalDurationMinutes()>0?g.getTotalDurationMinutes():durationMinutes(services);
        return GroomingAppointmentDTO.builder().id(g.getId()).petId(g.getPet().getId()).petName(g.getPet().getName()).ownerId(g.getOwner().getId()).ownerName(g.getOwner().getFullName())
                .groomerId(g.getGroomer()!=null?g.getGroomer().getId():null).groomerName(g.getGroomer()!=null?g.getGroomer().getFullName():null)
                .groomerProfileImage(g.getGroomer()!=null?g.getGroomer().getProfileImage():null)
                .serviceId(primary!=null?primary.getId():null).serviceName(label).serviceIds(ids).serviceNames(names).totalDurationMinutes(duration)
                .appointmentDate(g.getAppointmentDate()).appointmentTime(g.getAppointmentTime())
                .status(g.getStatus().name()).comfortProfile(g.getComfortProfile()).preferredFinish(g.getPreferredFinish())
                .completedTasks(GroomingVisitWorkflow.completed(g)).visitTasks(GroomingVisitWorkflow.tasks(g))
                .progressLabel(GroomingVisitWorkflow.progress(g)).taskTrackingEnabled(g.getCompletedTasks()!=null)
                .productsUsed(g.getProductsUsed()).nextVisitDate(g.getNextVisitDate())
                .specialInstructions(g.getSpecialInstructions()).totalPrice(g.getTotalPrice()).notes(g.getNotes()).build();
    }
}
