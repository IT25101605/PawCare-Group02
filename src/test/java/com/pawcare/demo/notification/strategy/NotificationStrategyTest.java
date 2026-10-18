package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.entity.Notification;
import com.pawcare.demo.service.NotificationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/** Focused Strategy Pattern regression tests; no running server or database required. */
class NotificationStrategyTest {
    @Test
    void contextSwitchesStrategiesAtRuntime() {
        NotificationContext context = new NotificationContext(new AppointmentReminderStrategy());
        assertEquals("Your appointment with Dr. Silva is tomorrow at 10:30.",
                context.format(new NotificationEvent("Owner", "Dr. Silva", "10:30", "tomorrow")).message());

        context.setStrategy(new VaccinationReminderStrategy());
        assertEquals("Buddy's Rabies vaccination is due in 7 days.",
                context.format(new NotificationEvent("Owner", "Buddy", "Rabies", "in 7 days")).message());

        context.setStrategy(new OrderStatusStrategy());
        assertEquals("Your order ORD-1001 is now shipped.",
                context.format(new NotificationEvent("Owner", "ORD-1001", "SHIPPED", "")).message());
        assertEquals(Notification.NotificationType.DELIVERY,
                context.format(new NotificationEvent("Owner", "ORD-1001", "OUT_FOR_DELIVERY", "")).type());
    }

    @Test
    void dispatcherDelegatesToExistingNotificationServiceWithOriginalMetadata() {
        NotificationService storage = mock(NotificationService.class);
        NotificationDispatchService dispatcher = new NotificationDispatchService(storage, strategies());

        dispatcher.dispatch(NotificationScenario.ORDER_STATUS, 42L,
                new NotificationEvent("Owner", "ORD-1001", "OUT_FOR_DELIVERY", ""),
                "/pages/dashboard/owner.html#orders", 8L, "ORDER", "order-status-8-OUT_FOR_DELIVERY");

        verify(storage).create(42L, "Order out for delivery", "Your PawCare order is out for delivery.",
                Notification.NotificationType.DELIVERY, Notification.Priority.INFO,
                "/pages/dashboard/owner.html#orders", 8L, "ORDER", "order-status-8-OUT_FOR_DELIVERY");
        verifyNoMoreInteractions(storage);
    }

    @Test
    void missingOrDuplicateImplementationsFailFast() {
        NotificationService storage = mock(NotificationService.class);
        assertThrows(IllegalStateException.class,
                () -> new NotificationDispatchService(storage, List.of(new AppointmentReminderStrategy())));
        assertThrows(IllegalStateException.class,
                () -> new NotificationDispatchService(storage, List.of(
                        new AppointmentReminderStrategy(), new AppointmentReminderStrategy(),
                        new VaccinationReminderStrategy(), new OrderStatusStrategy())));
    }

    @Test
    void nullStrategyIsRejected() {
        NotificationContext context = new NotificationContext(new AppointmentReminderStrategy());
        assertThrows(NullPointerException.class, () -> context.setStrategy(null));
    }

    private static List<NotificationStrategy> strategies() {
        return List.of(new AppointmentReminderStrategy(),
                new VaccinationReminderStrategy(), new OrderStatusStrategy());
    }
}
