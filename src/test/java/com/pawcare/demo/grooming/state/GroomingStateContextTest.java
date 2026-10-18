package com.pawcare.demo.grooming.state;

import com.pawcare.demo.entity.GroomingAppointment.GroomingStatus;
import com.pawcare.demo.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GroomingStateContextTest {
    private final GroomingStateContext context = new GroomingStateContext();

    @Test void initialStatusAndNormalSequence() {
        assertEquals(GroomingStatus.CONFIRMED, context.transition(GroomingStatus.SCHEDULED, "CONFIRMED"));
        assertEquals(GroomingStatus.IN_PROGRESS, context.transition(GroomingStatus.CONFIRMED, "IN_PROGRESS"));
        assertEquals(GroomingStatus.COMPLETED, context.transition(GroomingStatus.IN_PROGRESS, "COMPLETED"));
        // Groomer must accept before progressing a visit.
        assertThrows(BadRequestException.class, () -> context.transition(GroomingStatus.SCHEDULED, "IN_PROGRESS"));
    }

    @Test void cancellationsAndNoTerminalReopen() {
        for (GroomingStatus state : new GroomingStatus[] {GroomingStatus.SCHEDULED,
                GroomingStatus.CONFIRMED, GroomingStatus.IN_PROGRESS}) {
            assertEquals(GroomingStatus.CANCELLED, context.transition(state, "CANCELLED"));
        }
        for (GroomingStatus terminal : new GroomingStatus[] {GroomingStatus.COMPLETED, GroomingStatus.CANCELLED}) {
            assertThrows(BadRequestException.class, () -> context.transition(terminal, "IN_PROGRESS"));
            assertEquals(terminal, context.transition(terminal, terminal.name()));
        }
    }

    @Test void disallowBackwardJumpsAndInvalidStatus() {
        assertThrows(BadRequestException.class, () -> context.transition(GroomingStatus.CONFIRMED, "SCHEDULED"));
        assertThrows(BadRequestException.class, () -> context.transition(GroomingStatus.SCHEDULED, "COMPLETED"));
        assertThrows(BadRequestException.class, () -> context.transition(GroomingStatus.SCHEDULED, "UNKNOWN"));
    }

    @Test void ownerEditRules() {
        assertDoesNotThrow(() -> context.requireOwnerEditable(GroomingStatus.SCHEDULED));
        assertDoesNotThrow(() -> context.requireOwnerEditable(GroomingStatus.CONFIRMED));
        for (GroomingStatus restricted : new GroomingStatus[] {GroomingStatus.IN_PROGRESS,
                GroomingStatus.COMPLETED, GroomingStatus.CANCELLED}) {
            assertThrows(BadRequestException.class, () -> context.requireOwnerEditable(restricted));
        }
    }
}
