package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.PatientConsentResponse;
import com.mentesvivas.backend.entity.patient.ConsentScope;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.patient.PatientConsentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PatientConsentController {

    private final PatientConsentService consent;
    private final CurrentPatient patient;

    @GetMapping("/api/patients/me/consents")
    public List<PatientConsentResponse> mine() {
        return consent.listOwn(patient.get().getId());
    }

    @PutMapping("/api/patients/me/consents/{scope}")
    public PatientConsentResponse decide(@PathVariable ConsentScope scope, @Valid @RequestBody Decision body) {
        return consent.decideOwn(patient.get().getId(), scope, body.granted());
    }

    @GetMapping("/api/caregivers/me/patients/{patientId}/consents")
    public List<PatientConsentResponse> represented(@PathVariable UUID patientId) {
        return consent.listRepresented(patientId);
    }

    @PutMapping("/api/caregivers/me/patients/{patientId}/consents/{scope}")
    public PatientConsentResponse decideRepresented(@PathVariable UUID patientId, @PathVariable ConsentScope scope,
                                                    @Valid @RequestBody Decision body) {
        return consent.decideRepresented(patientId, scope, body.granted());
    }

    public record Decision(@NotNull Boolean granted) {}
}
