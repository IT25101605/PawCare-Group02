package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** Rules for a grooming booking currently in the IN_PROGRESS state. */
public final class InProgressGroomingState implements GroomingBookingState {
    @Override public GroomingStatus status() { return GroomingStatus.IN_PROGRESS; }
    @Override public boolean permits(GroomingStatus requested) { return requested == GroomingStatus.COMPLETED || requested == GroomingStatus.CANCELLED; }
}
