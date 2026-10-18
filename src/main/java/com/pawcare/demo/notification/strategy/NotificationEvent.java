package com.pawcare.demo.notification.strategy;

import java.util.Objects;

/** Inputs to a notification algorithm; unused fields may be empty. */
public record NotificationEvent(String recipientName, String subject, String detail, String when) {
    public NotificationEvent {
        Objects.requireNonNull(subject, "subject is required");
        Objects.requireNonNull(detail, "detail is required");
    }
}
