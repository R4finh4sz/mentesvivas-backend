package com.mentesvivas.backend.controller.prescription;

import com.jayway.jsonpath.JsonPath;
import com.mentesvivas.backend.entity.audit.AuditEvent;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.support.AdminTestSupport;
import com.mentesvivas.backend.support.AuthTestSupport;
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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.mentesvivas.backend.support.AuthTestSupport.bearer;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@ExtendWith(ClearTemporaryStorage.class)
@AutoConfigureMockMvc
@Transactional
class PrescriptionControllerTest {

    private static final String LOSARTANA = """
            {"medicationName":"Losartana","presentation":"50 mg comprimido","dose":"1 comprimido","route":"Oral",
             "frequency":"12 em 12 horas","duration":"30 dias","quantity":"60 comprimidos",
             "instructions":"Tomar apos as refeicoes","continuousUse":true}""";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private LinkTestSupport accounts;
    @Autowired
    private AdminTestSupport admin;
    @Autowired
    private CapturedEmailSender emails;
    @Autowired
    private AuditEventRepository events;

    private Account patient;
    private Account caregiver;
    private Account doctor;

    @BeforeEach
    void setUp() throws Exception {
        patient = accounts.patient("paciente@exemplo.com", "11144477735");
        caregiver = accounts.caregiver("cuidador@exemplo.com", "52998224725");
        doctor = accounts.doctor("dr.paulo@clinica.com", "12345678909", "Dr. Paulo Lima", true);
    }

    private void accept(String invitationPath, String linksPath, Account sender) throws Exception {
        mockMvc.perform(post(invitationPath).header("Authorization", bearer(sender.token()))
                .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"paciente@exemplo.com\"}"))
                .andExpect(status().isAccepted());
        String links = mockMvc.perform(get(linksPath).header("Authorization", bearer(patient.token())))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(links, "$[0].id");
        mockMvc.perform(post(linksPath + "/" + id + "/accept").header("Authorization", bearer(patient.token())))
                .andExpect(status().isOk());
    }

    private void linkDoctor() throws Exception {
        accept("/api/doctor-links/invitations", "/api/doctor-links", doctor);
    }

    private void linkCaregiver() throws Exception {
        accept("/api/caregiver-links/invitations", "/api/caregiver-links", caregiver);
    }

    private String doctorPath() {
        return "/api/doctors/me/patients/" + patient.id() + "/prescriptions";
    }

    private String carePath() {
        return "/api/care/patients/" + patient.id() + "/prescriptions";
    }

    private String body(String... items) {
        return "{\"orientations\":\"Retorno em 30 dias\",\"items\":[" + String.join(",", items) + "]}";
    }

    private ResultActions create(String body) throws Exception {
        return mockMvc.perform(post(doctorPath()).header("Authorization", bearer(doctor.token()))
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private String createdId(String body) throws Exception {
        String response = create(body).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(response, "$.id");
    }

    private ResultActions sign(String id, String password, String code) throws Exception {
        return mockMvc.perform(post("/api/doctors/me/prescriptions/" + id + "/sign")
                .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                .content("{\"password\":\"%s\",\"code\":\"%s\"}".formatted(password, code)));
    }

    private ResultActions challengeAndSign(String id) throws Exception {
        mockMvc.perform(post("/api/doctors/me/prescriptions/" + id + "/signature-challenge")
                        .header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk());
        return sign(id, AuthTestSupport.PASSWORD, emails.lastCode());
    }

    private String signedId() throws Exception {
        String id = createdId(body(LOSARTANA));
        challengeAndSign(id).andExpect(status().isOk());
        return id;
    }

    private ResultActions doctorGet(String id) throws Exception {
        return mockMvc.perform(get("/api/doctors/me/prescriptions/" + id)
                .header("Authorization", bearer(doctor.token())));
    }

    private void addPatientAllergy(String description) throws Exception {
        mockMvc.perform(post("/api/patients/me/allergies").header("Authorization", bearer(patient.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"category\":\"MEDICATION\",\"description\":\"" + description + "\",\"severity\":\"SEVERE\"}"))
                .andExpect(status().isCreated());
    }

    private void addDrug(String name, String ingredient, String therapeuticClass, boolean controlled) throws Exception {
        mockMvc.perform(post("/api/admin/drugs").header("Authorization", bearer(admin.token()))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"%s\",\"activeIngredient\":\"%s\",\"therapeuticClass\":\"%s\",\"controlled\":%s}"
                        .formatted(name, ingredient, therapeuticClass, controlled)))
                .andExpect(status().isCreated());
    }

    @Test
    void doctorWithoutAnActiveLinkCannotOpenAPrescription() throws Exception {
        create(body(LOSARTANA)).andExpect(status().isForbidden());
    }

    @Test
    void draftListsTheMissingFieldsAndWarnsThatTheAllergyStatusIsNotInformed() throws Exception {
        linkDoctor();

        create(body("{\"medicationName\":\"Losartana\",\"presentation\":\"50 mg\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.patientName").exists())
                .andExpect(jsonPath("$.doctorRegistry").value("CRM 123456/SP"))
                .andExpect(jsonPath("$.missingFields").value(hasItem("items[0].dose")))
                .andExpect(jsonPath("$.missingFields").value(hasItem("items[0].quantity")))
                .andExpect(jsonPath("$.warnings").value(hasItem("ALLERGY_NOT_INFORMED")))
                .andExpect(jsonPath("$.verificationCode").doesNotExist());
    }

    @Test
    void signingAnIncompleteDraftIsRefusedListingWhatToFix() throws Exception {
        linkDoctor();
        String id = createdId(body("{\"medicationName\":\"Losartana\",\"presentation\":\"50 mg\"}"));

        mockMvc.perform(post("/api/doctors/me/prescriptions/" + id + "/signature-challenge")
                .header("Authorization", bearer(doctor.token()))).andExpect(status().isOk());
        sign(id, AuthTestSupport.PASSWORD, emails.lastCode())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.missingFields").value(hasItem("items[0].route")));
        doctorGet(id).andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void signedPrescriptionGetsAPdfWithVerificationCodeValidityAndNotification() throws Exception {
        linkDoctor();
        String id = createdId(body(LOSARTANA));

        challengeAndSign(id)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SIGNED"))
                .andExpect(jsonPath("$.signatureSimulated").value(true))
                .andExpect(jsonPath("$.verificationCode").isNotEmpty())
                .andExpect(jsonPath("$.verificationUrl").value(org.hamcrest.Matchers.startsWith("https://verifica")))
                .andExpect(jsonPath("$.validUntil").isNotEmpty())
                .andExpect(jsonPath("$.missingFields.length()").value(0));

        byte[] pdf = mockMvc.perform(get("/api/doctors/me/prescriptions/" + id + "/pdf")
                        .header("Authorization", bearer(doctor.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        mockMvc.perform(get("/api/notifications").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$[?(@.type == 'PRESCRIPTION_AVAILABLE')].referenceId").value(id));
        assertThat(events.findByActorUserIdOrderByOccurredAtDesc(doctor.id()).stream().map(AuditEvent::getEventType))
                .contains(AuditEventType.PRESCRIPTION_SIGNED);
    }

    @Test
    void wrongPasswordOrWrongCodeKeepsTheDraftUnsigned() throws Exception {
        linkDoctor();
        String id = createdId(body(LOSARTANA));
        mockMvc.perform(post("/api/doctors/me/prescriptions/" + id + "/signature-challenge")
                .header("Authorization", bearer(doctor.token()))).andExpect(status().isOk());

        sign(id, "SenhaErrada1", emails.lastCode()).andExpect(status().isForbidden());
        sign(id, AuthTestSupport.PASSWORD, "000000").andExpect(status().isBadRequest());
        doctorGet(id).andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void repeatingTheSignatureDoesNotCreateAnotherDocument() throws Exception {
        linkDoctor();
        String id = signedId();
        String code = JsonPath.read(doctorGet(id).andReturn().getResponse().getContentAsString(),
                "$.verificationCode");

        sign(id, AuthTestSupport.PASSWORD, "000000")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verificationCode").value(code));
        mockMvc.perform(get(doctorPath()).header("Authorization", bearer(doctor.token())))
                .andExpect(jsonPath("$.length()").value(1));
    }

    @Test
    void signedPrescriptionIsNeverAltered() throws Exception {
        linkDoctor();
        String id = signedId();

        mockMvc.perform(put("/api/doctors/me/prescriptions/" + id).header("Authorization", bearer(doctor.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(LOSARTANA)))
                .andExpect(status().isConflict());
    }

    @Test
    void draftCanBeEditedUntilItIsSigned() throws Exception {
        linkDoctor();
        String id = createdId(body("{\"medicationName\":\"Losartana\"}"));

        mockMvc.perform(put("/api/doctors/me/prescriptions/" + id).header("Authorization", bearer(doctor.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(LOSARTANA)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].dose").value("1 comprimido"))
                .andExpect(jsonPath("$.missingFields.length()").value(0));
    }

    @Test
    void patientAndCaregiverNeverSeeDraftsButSeeTheSignedPrescriptionAndPdf() throws Exception {
        linkDoctor();
        linkCaregiver();
        String draft = createdId(body(LOSARTANA));

        mockMvc.perform(get(carePath()).header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(0));
        mockMvc.perform(get(carePath() + "/" + draft).header("Authorization", bearer(patient.token())))
                .andExpect(status().isNotFound());

        challengeAndSign(draft).andExpect(status().isOk());
        byte[] fromDoctor = mockMvc.perform(get("/api/doctors/me/prescriptions/" + draft + "/pdf")
                .header("Authorization", bearer(doctor.token()))).andReturn().getResponse().getContentAsByteArray();

        mockMvc.perform(get(carePath()).header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].doctorName").value("Dr. Paulo Lima"));
        byte[] fromCaregiver = mockMvc.perform(get(carePath() + "/" + draft + "/pdf")
                        .header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(fromCaregiver).isEqualTo(fromDoctor);
    }

    @Test
    void caregiverReadingIsRegisteredInTheAccessLog() throws Exception {
        linkDoctor();
        linkCaregiver();
        signedId();

        mockMvc.perform(get(carePath()).header("Authorization", bearer(caregiver.token()))).andExpect(status().isOk());

        mockMvc.perform(get("/api/patients/me/access-log").header("Authorization", bearer(patient.token())))
                .andExpect(jsonPath("$.items[?(@.area == 'PRESCRIPTIONS')].accessorId")
                        .value(hasItem(caregiver.id().toString())));
    }

    @Test
    void unlinkedCaregiverAndOtherDoctorCannotReadThePrescriptions() throws Exception {
        linkDoctor();
        String id = signedId();
        Account otherDoctor = accounts.doctor("dra.ana@clinica.com", "39053344705", "Dra. Ana", true);

        mockMvc.perform(get(carePath()).header("Authorization", bearer(caregiver.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/doctors/me/prescriptions/" + id).header("Authorization", bearer(otherDoctor.token())))
                .andExpect(status().isNotFound());
    }

    @Test
    void publicVerificationShowsOnlyTheStatus() throws Exception {
        linkDoctor();
        String id = signedId();
        String code = JsonPath.read(doctorGet(id).andReturn().getResponse().getContentAsString(),
                "$.verificationCode");

        mockMvc.perform(get("/api/public/prescriptions/" + code))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALID"))
                .andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/api/public/prescriptions/codigo-inexistente")).andExpect(status().isNotFound());
    }

    @Test
    void replacementKeepsTheOriginalAndMarksItReplacedOnlyWhenTheNewOneIsSigned() throws Exception {
        linkDoctor();
        String original = signedId();
        String originalCode = JsonPath.read(doctorGet(original).andReturn().getResponse().getContentAsString(),
                "$.verificationCode");

        String response = mockMvc.perform(post("/api/doctors/me/prescriptions/" + original + "/replace")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"Ajuste de dose\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andExpect(jsonPath("$.replacesId").value(original))
                .andExpect(jsonPath("$.replacementJustification").value("Ajuste de dose"))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andReturn().getResponse().getContentAsString();
        String replacement = JsonPath.read(response, "$.id");

        mockMvc.perform(get("/api/public/prescriptions/" + originalCode))
                .andExpect(jsonPath("$.status").value("VALID"));

        challengeAndSign(replacement).andExpect(status().isOk());

        mockMvc.perform(get("/api/public/prescriptions/" + originalCode))
                .andExpect(jsonPath("$.status").value("REPLACED"));
        doctorGet(original).andExpect(jsonPath("$.replaced").value(true))
                .andExpect(jsonPath("$.replacedById").value(replacement));
        mockMvc.perform(post("/api/doctors/me/prescriptions/" + original + "/replace")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"Outra\"}"))
                .andExpect(status().isConflict());
        assertThat(events.findByActorUserIdOrderByOccurredAtDesc(doctor.id()).stream().map(AuditEvent::getEventType))
                .contains(AuditEventType.PRESCRIPTION_REPLACED);
    }

    @Test
    void draftCannotBeReplacedAndTheReplacementNeedsAJustification() throws Exception {
        linkDoctor();
        String draft = createdId(body(LOSARTANA));
        String signed = signedId();

        mockMvc.perform(post("/api/doctors/me/prescriptions/" + draft + "/replace")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"x\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/doctors/me/prescriptions/" + signed + "/replace")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void allergyToAnItemBlocksThePrescription() throws Exception {
        linkDoctor();
        addPatientAllergy("Dipirona");

        create(body("{\"medicationName\":\"Dipirona\",\"presentation\":\"500 mg\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Alerta impeditivo")));
    }

    @Test
    void allergyToTheTherapeuticClassBlocksThroughTheDrugCatalog() throws Exception {
        linkDoctor();
        addDrug("Amoxicilina", "Amoxicilina tri-hidratada", "Antibiotico", false);
        addPatientAllergy("Antibioticos");

        create(body("{\"medicationName\":\"Amoxicilina\"}")).andExpect(status().isConflict());
    }

    @Test
    void controlledDrugIsRefusedWithGuidanceToUseTheOutOfAppFlow() throws Exception {
        linkDoctor();
        addDrug("Clonazepam", "Clonazepam", "Benzodiazepinico", true);

        create(body("{\"medicationName\":\"Clonazepam\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("fora do aplicativo")));
    }

    @Test
    void examGuideNeedsOrientationsAndFollowsTheSameSignatureFlow() throws Exception {
        linkDoctor();
        String empty = createdId("{\"kind\":\"EXAM_GUIDE\"}");
        doctorGet(empty).andExpect(jsonPath("$.missingFields").value(hasItem("orientations")));

        String id = createdId("{\"kind\":\"EXAM_GUIDE\",\"orientations\":\"Hemograma completo em jejum\"}");
        challengeAndSign(id).andExpect(status().isOk()).andExpect(jsonPath("$.kind").value("EXAM_GUIDE"));
    }

    @Test
    void caregiverMovesAPrescribedItemIntoTheRoutineKeepingTheOrigin() throws Exception {
        linkDoctor();
        linkCaregiver();
        String id = signedId();
        String itemId = JsonPath.read(doctorGet(id).andReturn().getResponse().getContentAsString(), "$.items[0].id");
        String path = carePath() + "/" + id + "/items/" + itemId + "/routine";
        String routine = "{\"unitsPerDose\":1,\"times\":[\"08:00\",\"20:00\"],\"stockQuantity\":60}";

        mockMvc.perform(post(path).header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(routine))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Losartana"))
                .andExpect(jsonPath("$.origin").value("PRESCRIPTION"));
        mockMvc.perform(post(path).header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(routine))
                .andExpect(status().isConflict());
    }

    @Test
    void replacedPrescriptionCannotEnterTheRoutine() throws Exception {
        linkDoctor();
        String original = signedId();
        String itemId = JsonPath.read(doctorGet(original).andReturn().getResponse().getContentAsString(),
                "$.items[0].id");
        String replacement = JsonPath.read(mockMvc.perform(post("/api/doctors/me/prescriptions/" + original + "/replace")
                        .header("Authorization", bearer(doctor.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"justification\":\"Ajuste\"}")).andReturn().getResponse().getContentAsString(),
                "$.id");
        challengeAndSign(replacement).andExpect(status().isOk());

        mockMvc.perform(post(carePath() + "/" + original + "/items/" + itemId + "/routine")
                        .header("Authorization", bearer(patient.token())).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"unitsPerDose\":1,\"times\":[\"08:00\"],\"stockQuantity\":30}"))
                .andExpect(status().isConflict());
    }

    @Test
    void patientAndCaregiverCannotUseTheDoctorEndpoints() throws Exception {
        mockMvc.perform(get(doctorPath()).header("Authorization", bearer(patient.token())))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(doctorPath()).header("Authorization", bearer(caregiver.token()))
                        .contentType(MediaType.APPLICATION_JSON).content(body(LOSARTANA)))
                .andExpect(status().isForbidden());
    }

    @Test
    void anonymousRequestsAreUnauthorizedExceptThePublicVerification() throws Exception {
        mockMvc.perform(get(carePath())).andExpect(status().isUnauthorized());
        mockMvc.perform(get(doctorPath())).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/public/prescriptions/x")).andExpect(status().isNotFound());
    }

    @Test
    void itemFieldsAreValidated() throws Exception {
        linkDoctor();
        create(body("{\"medicationName\":\"" + "a".repeat(151) + "\"}")).andExpect(status().isBadRequest());
        create("{\"items\":[" + String.join(",", java.util.Collections.nCopies(31, LOSARTANA)) + "]}")
                .andExpect(status().isBadRequest());
        assertThat(List.of(LOSARTANA)).hasSize(1);
    }
}
