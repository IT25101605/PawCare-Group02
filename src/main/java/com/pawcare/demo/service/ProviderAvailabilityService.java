package com.pawcare.demo.service;

import com.pawcare.demo.entity.*;
import com.pawcare.demo.exception.*;
import com.pawcare.demo.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.util.*;

@Service
@Transactional(readOnly = true)
public class ProviderAvailabilityService {
    private final ProviderAvailabilityRepository repository;
    private final UserRepository userRepository;
    private final AppointmentRepository appointmentRepository;
    private final GroomingAppointmentRepository groomingRepository;
    private final ProviderUnavailableDateRepository unavailableDateRepository;

    public ProviderAvailabilityService(ProviderAvailabilityRepository repository,
                                       UserRepository userRepository,
                                       AppointmentRepository appointmentRepository,
                                       GroomingAppointmentRepository groomingRepository,
                                       ProviderUnavailableDateRepository unavailableDateRepository) {
        this.repository = repository;
        this.userRepository = userRepository;
        this.appointmentRepository = appointmentRepository;
        this.groomingRepository = groomingRepository;
        this.unavailableDateRepository = unavailableDateRepository;
    }

    public List<Map<String,Object>> getForProvider(Long providerId) {
        User provider = provider(providerId);
        Map<DayOfWeek,ProviderAvailability> configured = new EnumMap<>(DayOfWeek.class);
        repository.findByProviderIdOrderByDayOfWeek(providerId).forEach(a -> configured.put(a.getDayOfWeek(), a));
        List<Map<String,Object>> result = new ArrayList<>();
        for (DayOfWeek day : DayOfWeek.values()) {
            ProviderAvailability a = configured.get(day);
            boolean weekday = true;
            result.add(Map.of(
                    "dayOfWeek", day.name(),
                    "startTime", a != null ? a.getStartTime().toString() : "08:00",
                    "endTime", a != null ? a.getEndTime().toString() : "17:00",
                    "active", a != null ? Boolean.TRUE.equals(a.getActive()) : weekday,
                    "providerId", provider.getId()
            ));
        }
        return result;
    }

    @Transactional
    public Map<String,Object> saveDay(Long providerId, String dayName, LocalTime start, LocalTime end, boolean active) {
        User provider = provider(providerId);
        assertCurrentProviderOrAdmin(provider);
        DayOfWeek day;
        try { day = DayOfWeek.valueOf(dayName.toUpperCase(Locale.ROOT)); }
        catch (Exception ex) { throw new BadRequestException("Invalid day of week"); }
        if (start == null || end == null || !start.isBefore(end))
            throw new BadRequestException("Availability start time must be before end time");
        ProviderAvailability a = repository.findByProviderIdAndDayOfWeek(providerId, day)
                .orElseGet(() -> ProviderAvailability.builder().provider(provider).dayOfWeek(day).build());
        a.setStartTime(start); a.setEndTime(end); a.setActive(active);
        repository.save(a);
        return Map.of("providerId", providerId, "dayOfWeek", day.name(), "startTime", start.toString(),
                "endTime", end.toString(), "active", active);
    }

    public boolean isAvailable(User provider, LocalDate date, LocalTime time) {
        return isAvailable(provider, date, time, 30);
    }

    public boolean isAvailable(User provider, LocalDate date, LocalTime time, int durationMinutes) {
        if (date == null || time == null) return false;
        if (durationMinutes < 1 || durationMinutes > 480) return false;
        if (unavailableDateRepository.existsByProviderIdAndUnavailableDate(provider.getId(), date)) return false;
        Optional<ProviderAvailability> configured = repository.findByProviderIdAndDayOfWeek(provider.getId(), date.getDayOfWeek());
        LocalTime start = configured.map(ProviderAvailability::getStartTime).orElse(LocalTime.of(8,0));
        LocalTime end = configured.map(ProviderAvailability::getEndTime).orElse(LocalTime.of(17,0));
        boolean active = configured.map(a -> Boolean.TRUE.equals(a.getActive())).orElse(true);
        if (!active || time.isBefore(start)) return false;
        LocalDateTime finish = LocalDateTime.of(date, time).plusMinutes(durationMinutes);
        return !finish.toLocalDate().isAfter(date) && !finish.toLocalTime().isAfter(end);
    }

    public List<String> availableSlots(Long providerId, LocalDate date, String kind, Long excludeId) {
        return availableSlots(providerId, date, kind, excludeId, 30);
    }

    public List<String> availableSlots(Long providerId, LocalDate date, String kind, Long excludeId, Integer durationMinutes) {
        User provider = provider(providerId);
        if (unavailableDateRepository.existsByProviderIdAndUnavailableDate(providerId,date)) return List.of();
        String role = provider.getRole().getName();
        if ("vet".equalsIgnoreCase(kind) && !"VETERINARIAN".equals(role)) throw new BadRequestException("Selected user is not a veterinarian");
        if ("groomer".equalsIgnoreCase(kind) && !"GROOMER".equals(role)) throw new BadRequestException("Selected user is not a groomer");

        Optional<ProviderAvailability> configured = repository.findByProviderIdAndDayOfWeek(providerId, date.getDayOfWeek());
        boolean active = configured.map(a -> Boolean.TRUE.equals(a.getActive())).orElse(true);
        if (!active) return List.of();
        LocalTime start = configured.map(ProviderAvailability::getStartTime).orElse(LocalTime.of(8,0));
        LocalTime end = configured.map(ProviderAvailability::getEndTime).orElse(LocalTime.of(17,0));
        int duration = "groomer".equalsIgnoreCase(kind) ? Math.max(15, Math.min(durationMinutes == null ? 30 : durationMinutes, 480)) : 30;
        LocalDateTime now = LocalDateTime.now();
        List<GroomingAppointment> groomerBookings = "groomer".equalsIgnoreCase(kind)
                ? groomingRepository.findActiveForGroomerDate(providerId, date) : List.of();
        List<String> slots = new ArrayList<>();
        for (LocalDateTime candidate = date.atTime(start); candidate.isBefore(date.atTime(end)); candidate = candidate.plusMinutes(30)) {
            LocalTime t = candidate.toLocalTime();
            if (!LocalDateTime.of(date,t).isAfter(now)) continue;
            if (LocalDateTime.of(date,t).plusMinutes(duration).toLocalTime().isAfter(end)
                    || LocalDateTime.of(date,t).plusMinutes(duration).toLocalDate().isAfter(date)) continue;
            boolean booked;
            if ("groomer".equalsIgnoreCase(kind)) {
                LocalTime candidateEnd=t.plusMinutes(duration);
                booked=false;
                for(GroomingAppointment g:groomerBookings){
                    if(excludeId!=null&&excludeId>0&&excludeId.equals(g.getId())) continue;
                    int otherDuration=groomingDuration(g);
                    LocalTime otherStart=g.getAppointmentTime();
                    if(otherStart==null)continue;
                    LocalTime otherEnd=otherStart.plusMinutes(otherDuration);
                    if(t.isBefore(otherEnd)&&otherStart.isBefore(candidateEnd)){booked=true;break;}
                }
            } else {
                booked=appointmentRepository.existsActiveSlot(providerId, date, t, excludeId == null ? -1L : excludeId);
            }
            if (!booked) slots.add(t.toString());
        }
        return slots;
    }

    private int groomingDuration(GroomingAppointment appointment) {
        Integer stored=appointment.getTotalDurationMinutes();
        if(stored!=null&&stored>0)return Math.max(15,stored);
        return appointment.getService()!=null&&appointment.getService().getDurationMinutes()!=null
                ? Math.max(15,appointment.getService().getDurationMinutes()) : 30;
    }

    public List<Map<String,Object>> getUnavailableDates(Long providerId) {
        provider(providerId);
        return unavailableDateRepository.findByProviderIdAndUnavailableDateGreaterThanEqualOrderByUnavailableDate(providerId,LocalDate.now())
                .stream().map(d -> Map.<String,Object>of("id",d.getId(),"date",d.getUnavailableDate().toString(),"reason",d.getReason()==null?"":d.getReason())).toList();
    }

    @Transactional
    public Map<String,Object> addUnavailableDate(Long providerId, LocalDate date, String reason) {
        User p=provider(providerId);assertCurrentProviderOrAdmin(p);
        if(date==null||date.isBefore(LocalDate.now()))throw new BadRequestException("Unavailable date cannot be in the past");
        if(unavailableDateRepository.existsByProviderIdAndUnavailableDate(providerId,date))throw new BadRequestException("This date is already blocked");
        ProviderUnavailableDate saved=unavailableDateRepository.save(ProviderUnavailableDate.builder().provider(p).unavailableDate(date).reason(reason==null?null:reason.strip()).build());
        return Map.of("id",saved.getId(),"date",saved.getUnavailableDate().toString(),"reason",saved.getReason()==null?"":saved.getReason());
    }

    @Transactional
    public void deleteUnavailableDate(Long providerId,Long id) {
        User p=provider(providerId);assertCurrentProviderOrAdmin(p);
        ProviderUnavailableDate d=unavailableDateRepository.findByIdAndProviderId(id,providerId)
                .orElseThrow(() -> new ResourceNotFoundException("UnavailableDate","id",id));
        unavailableDateRepository.delete(d);
    }

    private User provider(Long id) {
        User u = userRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("User","id",id));
        if (!List.of("VETERINARIAN","GROOMER").contains(u.getRole().getName()) || !Boolean.TRUE.equals(u.getEnabled()))
            throw new BadRequestException("Choose an active veterinarian or groomer");
        return u;
    }

    private void assertCurrentProviderOrAdmin(User provider) {
        if (SecurityService.hasRole("ADMIN")) return;
        if (!new SecurityService().isCurrentUser(provider.getId()))
            throw new org.springframework.security.access.AccessDeniedException("You can only edit your own availability");
    }
}
