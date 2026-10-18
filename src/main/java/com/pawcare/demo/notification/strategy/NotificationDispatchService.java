package com.pawcare.demo.notification.strategy;

import com.pawcare.demo.dto.NotificationDTO;
import com.pawcare.demo.service.NotificationService;
import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/** Spring adapter from design-pattern strategies to PawCare's current notification storage. */
@Service
public class NotificationDispatchService {
    private final NotificationService notificationService;
    private final Map<NotificationScenario, NotificationStrategy> strategies;

    public NotificationDispatchService(NotificationService notificationService, List<NotificationStrategy> implementations) {
        this.notificationService = notificationService;
        EnumMap<NotificationScenario, NotificationStrategy> registered = new EnumMap<>(NotificationScenario.class);
        for (NotificationStrategy implementation : implementations) {
            NotificationStrategy existing = registered.putIfAbsent(implementation.supports(), implementation);
            if (existing != null) {
                throw new IllegalStateException("Duplicate notification strategy: " + implementation.supports());
            }
        }
        for (NotificationScenario scenario : NotificationScenario.values()) {
            if (!registered.containsKey(scenario)) {
                throw new IllegalStateException("Missing notification strategy: " + scenario);
            }
        }
        this.strategies = Map.copyOf(registered);
    }

    public NotificationDTO dispatch(NotificationScenario scenario, Long userId, NotificationEvent event,
                                    String link, Long relatedEntityId, String relatedEntityType, String dedupeKey) {
        Objects.requireNonNull(scenario, "notification scenario is required");
        // A fresh context for every dispatch prevents strategy changes leaking across concurrent users.
        NotificationContext context = new NotificationContext(strategies.get(scenario));
        NotificationContent content = context.format(event);
        return notificationService.create(userId, content.title(), content.message(), content.type(),
                content.priority(), link, relatedEntityId, relatedEntityType, dedupeKey);
    }
}
