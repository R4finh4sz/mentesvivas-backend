package com.mentesvivas.backend.controller.link;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.support.CapturedEmailSender;
import com.mentesvivas.backend.support.ClearTemporaryStorage;
import com.mentesvivas.backend.support.LinkTestSupport;
import com.mentesvivas.backend.support.LinkTestSupport.Account;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class CaregiverLinkControllerTest {

    private static final String BASE = "/api/caregiver-links";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private CapturedEmailSender emails;

    private Account caregiver;
    private Account patient;

    @BeforeEach
    void setUp() throws Exception {
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        emails.clear();
    }

    @Test
    void patientAcceptsTheInvitationFromTheNotificationsAndTheCaregiverIsNotified() throws Exception {
        invite(caregiver, "paciente@exemplo.com").andExpect(status().isAccepted());

        assertThat(emails.sentEmails()).singleElement().satisfies(sent -> {
            assertThat(sent.recipient()).isEqualTo("paciente@exemplo.com");
            assertThat(sent.body()).doesNotContain("Pessoa Teste");
        });
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].type").value("CAREGIVER_INVITATION"))
                .andExpect(jsonPath("$[0].read").value(false));

        String linkId = onlyLinkId(patient);
        act(patient, linkId, "accept")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$[0].type").value("LINK_ACCEPTED"));
        mockMvc.perform(get(BASE).header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$[0].patient.id").value(patient.id().toString()));
    }

    @Test
    void invitationDoesNotCompleteTheLinkByItself() throws Exception {
        invite(caregiver, "paciente@exemplo.com");

        mockMvc.perform(get(BASE).header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$[0].status").value("PENDING"));
    }

    @Test
    void sameAnswerForUnknownEmailsAndNonPatientsAndNoLinkIsCreatedForThem() throws Exception {
        invite(caregiver, "ninguem@exemplo.com").andExpect(status().isAccepted());
        invite(caregiver, "cuidador@exemplo.com").andExpect(status().isAccepted());

        mockMvc.perform(get(BASE).header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    void invitingTwiceKeepsASingleLink() throws Exception {
        invite(caregiver, "paciente@exemplo.com");
        invite(caregiver, "paciente@exemplo.com");

        mockMvc.perform(get(BASE).header("Authorization", bearer(caregiver.token())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void onlyCaregiversInvite() throws Exception {
        invite(patient, "outro@exemplo.com").andExpect(status().isForbidden());
    }

    @Test
    void onlyTheInvitedPatientAnswers() throws Exception {
        Account other = accounts.patient("outro@exemplo.com", "39053344705");
        invite(caregiver, "paciente@exemplo.com");
        String linkId = onlyLinkId(patient);

        act(other, linkId, "accept").andExpect(status().isForbidden());
        act(caregiver, linkId, "accept").andExpect(status().isForbidden());
    }

    @Test
    void declinedInvitationCanBeInvitedAgainAndCannotBeAnsweredTwice() throws Exception {
        invite(caregiver, "paciente@exemplo.com");
        String linkId = onlyLinkId(patient);

        act(patient, linkId, "decline").andExpect(jsonPath("$.status").value("DECLINED"));
        act(patient, linkId, "accept").andExpect(status().isConflict());

        invite(caregiver, "paciente@exemplo.com");
        act(patient, linkId, "accept").andExpect(jsonPath("$.status").value("ACTIVE"));
    }

    @Test
    void eitherPartyEndsTheLinkAndTheOtherIsNotified() throws Exception {
        invite(caregiver, "paciente@exemplo.com");
        String linkId = onlyLinkId(patient);
        act(patient, linkId, "accept");

        act(caregiver, linkId, "end").andExpect(jsonPath("$.status").value("ENDED"));
        act(caregiver, linkId, "end").andExpect(status().isConflict());
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].type").value("LINK_ENDED"));
    }

    @Test
    void outsidersCannotEndTheLink() throws Exception {
        Account other = accounts.caregiver("intruso@exemplo.com", "39053344705");
        invite(caregiver, "paciente@exemplo.com");

        act(other, onlyLinkId(patient), "end").andExpect(status().isForbidden());
    }

    @Test
    void invitationToAnUnregisteredEmailIsLinkedOnlyAfterTheEmailIsConfirmed() throws Exception {
        invite(caregiver, "novo@exemplo.com").andExpect(status().isAccepted());
        assertThat(emails.sentEmails()).singleElement()
                .satisfies(sent -> assertThat(sent.recipient()).isEqualTo("novo@exemplo.com"));

        Account novo = accounts.patient("novo@exemplo.com", "39053344705");
        mockMvc.perform(get(BASE).header("Authorization", bearer(novo.token())))
                .andExpect(jsonPath("$.length()").value(0));

        accounts.confirmEmail("novo@exemplo.com");

        mockMvc.perform(get(BASE).header("Authorization", bearer(novo.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PENDING"))
                .andExpect(jsonPath("$[0].caregiver.id").value(caregiver.id().toString()));
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(novo.token())))
                .andExpect(jsonPath("$[0].type").value("CAREGIVER_INVITATION"));
    }

    @Test
    void notificationsCanBeMarkedAsReadOnlyByTheirOwner() throws Exception {
        invite(caregiver, "paciente@exemplo.com");
        String body = mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$[0].id");

        mockMvc.perform(post("/api/notifications/" + id + "/read").header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isNotFound());
        mockMvc.perform(post("/api/notifications/" + id + "/read").header("Authorization", bearer(patient.token())))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[0].read").value(true));
    }

    @Test
    void routesRequireAuthentication() throws Exception {
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/notifications")).andExpect(status().isUnauthorized());
    }

    private ResultActions invite(Account who, String email) throws Exception {
        return mockMvc.perform(post(BASE + "/invitations").header("Authorization", bearer(who.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"%s\"}".formatted(email)));
    }

    private ResultActions act(Account who, String linkId, String action) throws Exception {
        return mockMvc.perform(post(BASE + "/" + linkId + "/" + action).header("Authorization", bearer(who.token())));
    }

    private String onlyLinkId(Account who) throws Exception {
        String body = mockMvc.perform(get(BASE).header("Authorization", bearer(who.token())))
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$[0].id");
    }
}
