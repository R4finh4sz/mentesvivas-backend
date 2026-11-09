package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.PatientProfileResponse;
import com.mentesvivas.backend.dto.patient.SensitiveChangeResponse;
import com.mentesvivas.backend.dto.patient.UpdatePatientProfileRequest;
import com.mentesvivas.backend.service.patient.PatientProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/me")
@RequiredArgsConstructor
public class PatientProfileController {

    private final PatientProfileService patientProfileService;

    @GetMapping("/profile")
    public PatientProfileResponse get() {
        return patientProfileService.get();
    }

    @PutMapping("/profile")
    public PatientProfileResponse update(@Valid @RequestBody UpdatePatientProfileRequest body) {
        return patientProfileService.update(body);
    }

    @GetMapping("/changes")
    public List<SensitiveChangeResponse> changes() {
        return patientProfileService.changes();
    }
}
