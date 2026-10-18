package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.entity.Notification;
import org.springframework.stereotype.Component;
import java.util.Locale;

/** Concrete Strategy: pet-store order status notifications. */
@Component
public class OrderStatusStrategy implements NotificationStrategy {
    @Override public NotificationScenario supports() { return NotificationScenario.ORDER_STATUS; }

    @Override public NotificationContent format(NotificationEvent event) {
        String normalizedStatus = event.detail().replace('_', ' ').toLowerCase(Locale.ROOT);
        boolean isOutForDelivery = "OUT_FOR_DELIVERY".equalsIgnoreCase(event.detail());
        return new NotificationContent(
                isOutForDelivery ? "Order out for delivery" : "Order updated",
                isOutForDelivery ? "Your PawCare order is out for delivery."
                        : "Your order " + event.subject() + " is now " + normalizedStatus + ".",
                isOutForDelivery ? Notification.NotificationType.DELIVERY : Notification.NotificationType.ORDER,
                Notification.Priority.INFO);
    }
}
