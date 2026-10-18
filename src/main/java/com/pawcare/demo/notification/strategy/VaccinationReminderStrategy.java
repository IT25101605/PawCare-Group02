package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.entity.Notification;
import org.springframework.stereotype.Component;

/** Concrete Strategy: due and upcoming vaccination reminders. */
@Component
public class VaccinationReminderStrategy implements NotificationStrategy {
    @Override public NotificationScenario supports() { return NotificationScenario.VACCINATION_REMINDER; }

    @Override public NotificationContent format(NotificationEvent event) {
        return new NotificationContent("Vaccination due",
                event.subject() + "'s " + event.detail() + " vaccination is due " + event.when() + ".",
                Notification.NotificationType.VACCINATION, Notification.Priority.WARNING);
    }
}
