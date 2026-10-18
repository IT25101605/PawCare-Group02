package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.entity.Notification;
import org.springframework.stereotype.Component;

/** Concrete Strategy: veterinary appointment reminders. */
@Component
public class AppointmentReminderStrategy implements NotificationStrategy {
    @Override public NotificationScenario supports() { return NotificationScenario.APPOINTMENT_REMINDER; }

    @Override public NotificationContent format(NotificationEvent event) {
        return new NotificationContent("Veterinary appointment reminder",
                "Your appointment with " + event.subject() + " is " + event.when()
                        + " at " + event.detail() + ".",
                Notification.NotificationType.APPOINTMENT, Notification.Priority.WARNING);
    }
}
