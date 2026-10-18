package com.pawcare.demo.service;

import com.pawcare.demo.entity.*;
import com.pawcare.demo.repository.*;
import com.pawcare.demo.notification.strategy.NotificationDispatchService;
import com.pawcare.demo.notification.strategy.NotificationEvent;
import com.pawcare.demo.notification.strategy.NotificationScenario;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class NotificationScheduler {
    private final AppointmentRepository appointmentRepository;
    private final GroomingAppointmentRepository groomingRepository;
    private final VaccinationRepository vaccinationRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final NotificationService notifications;
    private final NotificationDispatchService strategyNotifications;

    public NotificationScheduler(AppointmentRepository appointmentRepository,
                                 GroomingAppointmentRepository groomingRepository,
                                 VaccinationRepository vaccinationRepository,
                                 MedicalRecordRepository medicalRecordRepository,
                                 NotificationService notifications,
                                 NotificationDispatchService strategyNotifications) {
        this.appointmentRepository = appointmentRepository;
        this.groomingRepository = groomingRepository;
        this.vaccinationRepository = vaccinationRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.notifications = notifications;
        this.strategyNotifications = strategyNotifications;
    }

    @Scheduled(cron = "0 5 * * * *")
    @Transactional
    public void scheduledRemindersAndStatuses() {
        LocalDate today = LocalDate.now();
        LocalDate through = today.plusDays(7);

        for (Appointment a : appointmentRepository.findByAppointmentDateBetween(today, through)) {
            long days = ChronoUnit.DAYS.between(today, a.getAppointmentDate());
            if (a.getStatus() == Appointment.AppointmentStatus.SCHEDULED || a.getStatus() == Appointment.AppointmentStatus.CONFIRMED) {
                if (days == 7 || days == 1 || days == 0) {
                    String when = days == 7 ? "in 7 days" : days == 0 ? "today" : "tomorrow";
                    strategyNotifications.dispatch(NotificationScenario.APPOINTMENT_REMINDER, a.getOwner().getId(),
                            new NotificationEvent(a.getOwner().getFullName(), a.getVeterinarian().getFullName(),
                                    String.valueOf(a.getAppointmentTime()), when),
                            "/pages/dashboard/owner.html#appointments", a.getId(), "APPOINTMENT",
                            "appt-reminder-" + a.getId() + "-" + today);
                }
            }
        }

        for (GroomingAppointment g : groomingRepository.findByAppointmentDateBetween(today, through)) {
            long days = ChronoUnit.DAYS.between(today, g.getAppointmentDate());
            if (g.getStatus() == GroomingAppointment.GroomingStatus.SCHEDULED || g.getStatus() == GroomingAppointment.GroomingStatus.CONFIRMED) {
                if (days == 7 || days == 1 || days == 0) {
                    String when = days == 7 ? "in 7 days" : days == 0 ? "today" : "tomorrow";
                    String groomerName = g.getGroomer() == null ? "your groomer" : g.getGroomer().getFullName();
                    notifications.create(g.getOwner().getId(), "Grooming reminder",
                            "Your grooming appointment with " + groomerName + " is " + when + " at " + g.getAppointmentTime() + ".",
                            Notification.NotificationType.GROOMING, Notification.Priority.WARNING,
                            "/pages/dashboard/owner.html#grooming", g.getId(), "GROOMING",
                            "groom-reminder-" + g.getId() + "-" + today);
                }
            }
        }

        for (Vaccination v : vaccinationRepository.findByNextDueDateBetween(today, through)) {
            if (v.getNextDueDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, v.getNextDueDate());
            if (days == 7 || days == 1 || days == 0) {
                String phrase = days == 7 ? "in 7 days" : days == 1 ? "tomorrow" : "today";
                strategyNotifications.dispatch(NotificationScenario.VACCINATION_REMINDER, v.getPet().getOwner().getId(),
                        new NotificationEvent(v.getPet().getOwner().getFullName(), v.getPet().getName(),
                                v.getVaccineName(), phrase),
                        "/pages/dashboard/owner.html#pets", v.getId(), "VACCINATION",
                        "vaccination-reminder-" + v.getId() + "-" + v.getNextDueDate() + "-" + days);
            }
        }

        for (MedicalRecord m : medicalRecordRepository.findByFollowUpDateBetween(today, today.plusDays(1))) {
            if (m.getFollowUpDate() == null) continue;
            long days = ChronoUnit.DAYS.between(today, m.getFollowUpDate());
            if (days == 1 || days == 0) {
                String phrase = days == 0 ? "today" : "tomorrow";
                notifications.create(m.getPet().getOwner().getId(), "Medical follow-up reminder",
                        m.getPet().getName() + "'s veterinary follow-up is " + phrase + ".",
                        Notification.NotificationType.MEDICAL, Notification.Priority.WARNING,
                        "/pages/dashboard/owner.html#pets", m.getId(), "MEDICAL_RECORD",
                        "medical-followup-" + m.getId() + "-" + m.getFollowUpDate() + "-" + days);
            }
        }

        markPastAppointments();
    }

    private void markPastAppointments() {
        LocalDate today = LocalDate.now();
        for (Appointment a : appointmentRepository.findByAppointmentDateBeforeAndStatusIn(
                today, java.util.List.of(Appointment.AppointmentStatus.SCHEDULED, Appointment.AppointmentStatus.CONFIRMED))) {
            a.setStatus(Appointment.AppointmentStatus.NO_SHOW);
            appointmentRepository.save(a);
            notifications.create(a.getOwner().getId(), "Appointment marked missed",
                    "The appointment for " + a.getPet().getName() + " on " + a.getAppointmentDate() + " was marked as missed.",
                    Notification.NotificationType.APPOINTMENT, Notification.Priority.INFO,
                    "/pages/dashboard/owner.html#appointments", a.getId(), "APPOINTMENT",
                    "appt-missed-" + a.getId());
        }
    }
}
