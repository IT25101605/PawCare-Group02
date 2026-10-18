package com.pawcare.demo.appointment.chain;

import com.pawcare.demo.entity.User;
import com.pawcare.demo.exception.BadRequestException;
import com.pawcare.demo.repository.AppointmentRepository;
import com.pawcare.demo.service.ProviderAvailabilityService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Chain of Responsibility: checks run in order and stop at the first failure. */
public final class AppointmentBookingValidationChain {
    private final ValidationRule first;

    public AppointmentBookingValidationChain(ProviderAvailabilityService availability,
                                              AppointmentRepository appointments) {
        ValidationRule future = new FutureTimeRule();
        ValidationRule provider = new ProviderAvailableRule(availability);
        ValidationRule overlap = new SlotConflictRule(appointments);
        future.link(provider).link(overlap);
        this.first = future;
    }

    public void validate(User veterinarian, LocalDate date, LocalTime time, Long excludeId) {
        first.check(new BookingRequest(veterinarian, date, time, excludeId));
    }

    private record BookingRequest(User veterinarian, LocalDate date, LocalTime time, Long excludeId) { }

    private abstract static class ValidationRule {
        private ValidationRule next;
        final ValidationRule link(ValidationRule follower) { next = follower; return follower; }
        final void check(BookingRequest request) {
            validate(request);
            if (next != null) next.check(request);
        }
        protected abstract void validate(BookingRequest request);
    }
    private static final class FutureTimeRule extends ValidationRule {
        @Override protected void validate(BookingRequest r) {
            if (r.date() == null || r.time() == null || !LocalDateTime.of(r.date(), r.time()).isAfter(LocalDateTime.now()))
                throw new BadRequestException("Choose an appointment date and time in the future");
        }
    }
    private static final class ProviderAvailableRule extends ValidationRule {
        private final ProviderAvailabilityService availability;
        ProviderAvailableRule(ProviderAvailabilityService availability) { this.availability = availability; }
        @Override protected void validate(BookingRequest r) {
            if (!availability.isAvailable(r.veterinarian(), r.date(), r.time()))
                throw new BadRequestException("The veterinarian is not available at this date and time");
        }
    }
    private static final class SlotConflictRule extends ValidationRule {
        private final AppointmentRepository appointments;
        SlotConflictRule(AppointmentRepository appointments) { this.appointments = appointments; }
        @Override protected void validate(BookingRequest r) {
            if (appointments.existsActiveSlot(r.veterinarian().getId(), r.date(), r.time(), r.excludeId()))
                throw new BadRequestException("This veterinarian time slot is already booked");
        }
    }
}
