package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;
import com.pawcare.demo.exception.BadRequestException;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * State Pattern context for GroomingAppointment's persisted status enum.
 * Stateless state objects keep the JPA entity and existing database schema unchanged.
 * Business authorization and visit-checklist validation stay with existing services.
 */
public final class GroomingStateContext {
    private final Map<GroomingStatus, GroomingBookingState> states = new EnumMap<>(GroomingStatus.class);

    public GroomingStateContext() {
        for (GroomingBookingState state : List.of(
                new ScheduledGroomingState(), new ConfirmedGroomingState(),
                new InProgressGroomingState(), new CompletedGroomingState(),
                new CancelledGroomingState())) {
            if (states.putIfAbsent(state.status(), state) != null) {
                throw new IllegalStateException("Duplicate grooming state: " + state.status());
            }
        }
        if (states.size() != GroomingStatus.values().length) {
            throw new IllegalStateException("Missing grooming state implementation");
        }
    }

    /** Validates the request, returning the selected state to persist on the entity. */
    public GroomingStatus transition(GroomingStatus current, String requested) {
        Objects.requireNonNull(current, "current grooming state");
        GroomingStatus target;
        try { target = GroomingStatus.valueOf(requested); }
        catch (IllegalArgumentException | NullPointerException ex) {
            throw new BadRequestException("Invalid grooming status");
        }
        if (current != target && !states.get(current).permits(target)) {
            throw new BadRequestException("Cannot change a " + readable(current.name())
                    + " visit to " + readable(requested));
        }
        return target;
    }

    public void requireOwnerEditable(GroomingStatus status) {
        Objects.requireNonNull(status, "grooming status");
        if (!states.get(status).ownerCanEdit()) {
            throw new BadRequestException("Only scheduled or confirmed bookings can be edited or cancelled");
        }
    }

    private static String readable(String code) {
        return code.toLowerCase(Locale.ROOT).replace('_', ' ');
    }
}
