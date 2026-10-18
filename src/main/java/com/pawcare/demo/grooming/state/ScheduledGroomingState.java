package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** Rules for a grooming booking currently in the SCHEDULED state. */
public final class ScheduledGroomingState implements GroomingBookingState {
    @Override public GroomingStatus status() { return GroomingStatus.SCHEDULED; }
    @Override public boolean permits(GroomingStatus requested) { return requested == GroomingStatus.CONFIRMED || requested == GroomingStatus.CANCELLED; }
    @Override public boolean ownerCanEdit() { return true; }
}
