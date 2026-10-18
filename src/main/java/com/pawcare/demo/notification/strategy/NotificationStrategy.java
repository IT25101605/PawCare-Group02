package com.pawcare.demo.notification.strategy;

/** Strategy interface. Each concrete implementation formats one notification type. */
public interface NotificationStrategy {
    NotificationScenario supports();
    NotificationContent format(NotificationEvent event);
}
