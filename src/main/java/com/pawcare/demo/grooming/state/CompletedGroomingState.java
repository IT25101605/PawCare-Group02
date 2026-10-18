package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** Rules for a grooming booking currently in the COMPLETED state. */
public final class CompletedGroomingState implements GroomingBookingState {
    @Override public GroomingStatus status() { return GroomingStatus.COMPLETED; }
    @Override public boolean permits(GroomingStatus requested) { return false; }
}
