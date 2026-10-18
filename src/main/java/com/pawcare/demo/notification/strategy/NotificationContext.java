package com.pawcare.demo.notification.strategy;

import java.util.Objects;

/** Context: delegates formatting to a Strategy; its behavior can change at runtime. */
public final class NotificationContext {
    private NotificationStrategy strategy;

    public NotificationContext(NotificationStrategy initialStrategy) {
        setStrategy(initialStrategy);
    }

    public void setStrategy(NotificationStrategy nextStrategy) {
        this.strategy = Objects.requireNonNull(nextStrategy, "strategy is required");
    }

    public NotificationContent format(NotificationEvent event) {
        return strategy.format(Objects.requireNonNull(event, "event is required"));
    }
}
