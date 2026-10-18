package com.pawcare.demo.notification.strategy;

/** Run with -ea to verify dynamic strategy switching without requiring a database. */
public class NotificationStrategyDemo {
    public static void main(String[] args) {
        NotificationContext context = new NotificationContext(new AppointmentReminderStrategy());
        NotificationContent appointment = context.format(new NotificationEvent("Pet owner", "Dr. Silva", "10:30", "tomorrow"));
        assert appointment.message().equals("Your appointment with Dr. Silva is tomorrow at 10:30.");
        System.out.println("Appointment: " + appointment.message());

        context.setStrategy(new VaccinationReminderStrategy());
        NotificationContent vaccine = context.format(new NotificationEvent("Pet owner", "Buddy", "Rabies", "in 7 days"));
        assert vaccine.message().equals("Buddy's Rabies vaccination is due in 7 days.");
        System.out.println("Vaccination: " + vaccine.message());

        context.setStrategy(new OrderStatusStrategy());
        NotificationContent order = context.format(new NotificationEvent("Pet owner", "ORD-1001", "SHIPPED", ""));
        assert order.message().equals("Your order ORD-1001 is now shipped.");
        System.out.println("Order: " + order.message());

        NotificationContent delivery = context.format(new NotificationEvent("Pet owner", "ORD-1001", "OUT_FOR_DELIVERY", ""));
        assert delivery.type() == com.pawcare.demo.entity.Notification.NotificationType.DELIVERY;
        System.out.println("Delivery: " + delivery.message());
        System.out.println("PASS: all notification strategies and runtime switch verified");
    }
}
