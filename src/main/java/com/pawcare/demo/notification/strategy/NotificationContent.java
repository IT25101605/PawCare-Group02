package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.entity.Notification;

/** Formatted, typed information persisted by the existing NotificationService. */
public record NotificationContent(String title, String message,
                                  Notification.NotificationType type,
                                  Notification.Priority priority) {}
