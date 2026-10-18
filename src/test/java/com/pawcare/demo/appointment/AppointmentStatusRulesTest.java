package com.pawcare.demo.appointment;
import com.pawcare.demo.service.AppointmentService;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import java.util.List;

class AppointmentStatusRulesTest {
    @Test void onlyForwardTransitions() {
        assertEquals(List.of("CONFIRMED","CANCELLED","NO_SHOW"), AppointmentService.allowedStatusTransitions("SCHEDULED"));
        assertEquals(List.of("IN_PROGRESS","CANCELLED","NO_SHOW"), AppointmentService.allowedStatusTransitions("CONFIRMED"));
        assertEquals(List.of("COMPLETED"), AppointmentService.allowedStatusTransitions("IN_PROGRESS"));
    }
    @Test void completedAndCancelledCannotBeReopened() {
        for (String status : List.of("COMPLETED","CANCELLED","NO_SHOW","INVALID"))
            assertTrue(AppointmentService.allowedStatusTransitions(status).isEmpty());
    }
}
