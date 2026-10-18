package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;

/** State contract: each appointment state controls its legal next states. */
public interface GroomingBookingState {
    GroomingStatus status();
    boolean permits(GroomingStatus requested);
    default boolean ownerCanEdit() { return false; }
}
