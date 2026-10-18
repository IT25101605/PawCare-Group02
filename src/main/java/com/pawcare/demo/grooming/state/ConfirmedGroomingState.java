package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** Rules for a grooming booking currently in the CONFIRMED state. */
public final class ConfirmedGroomingState implements GroomingBookingState {
    @Override public GroomingStatus status() { return GroomingStatus.CONFIRMED; }
    @Override public boolean permits(GroomingStatus requested) { return requested == GroomingStatus.IN_PROGRESS || requested == GroomingStatus.CANCELLED; }
    @Override public boolean ownerCanEdit() { return true; }
}
