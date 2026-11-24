package com.mentesvivas.backend.controller.notification;

import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.service.notification.NotificationService;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class NotificationPreferencesControllerTest {

    @Autowired
    private MockMvc mvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private NotificationService notifications;

    @Test
    void caregiverCanMuteRoutineNoticesAndCriticalAlertsKeepPriority() throws Exception {
        var patient = accounts.patient("paciente@exemplo.com", "11144477735");
        var caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        var other = accounts.caregiver("outro@exemplo.com", "39053344705");
        String route = "/api/caregivers/me/notification-preferences";
        String token = bearer(caregiver.token());

        mvc.perform(get(route).header("Authorization", bearer(patient.token())))
                .andExpect(status().isForbidden());
        mvc.perform(get(route).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.medicationEnabled").value(true));
        mvc.perform(put(route).header("Authorization", token).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"sundownEnabled":false,"medicationEnabled":false,"geofenceEnabled":false,
                                 "sundownTime":"19:00:00"}
                                """))
                .andExpect(status().isOk());
        mvc.perform(get(route).header("Authorization", token))
                .andExpect(status().isOk()).andExpect(jsonPath("$.medicationEnabled").value(false))
                .andExpect(jsonPath("$.sundownTime").value("19:00:00"));
        mvc.perform(get(route).header("Authorization", bearer(other.token())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.medicationEnabled").value(true));

        notifications.notify(caregiver.id(), NotificationType.MEDICATION_REMINDER, patient.id(), "Medicação");
        notifications.notify(caregiver.id(), NotificationType.SUNDOWN_REMINDER, patient.id(), "Anoitecer");
        notifications.notify(caregiver.id(), NotificationType.GEOFENCE_NOTICE, patient.id(), "Localização");
        notifications.notify(caregiver.id(), NotificationType.SOS_ALERT, patient.id(), "SOS");
        notifications.notify(caregiver.id(), NotificationType.BREATHING_STARTED, patient.id(), "Respiração");

        mvc.perform(get("/api/notifications").header("Authorization", token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].type").value("SOS_ALERT"))
                .andExpect(jsonPath("$[1].type").value("BREATHING_STARTED"));
    }
}
