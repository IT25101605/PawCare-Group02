package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** Rules for a grooming booking currently in the CANCELLED state. */
public final class CancelledGroomingState implements GroomingBookingState {
    @Override public GroomingStatus status() { return GroomingStatus.CANCELLED; }
    @Override public boolean permits(GroomingStatus requested) { return false; }
}
