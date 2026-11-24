package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.AutonomySettings;
import com.mentesvivas.backend.service.patient.PatientAutonomyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PatientAutonomyController {

    private final PatientAutonomyService autonomy;

    @GetMapping("/api/patients/me/autonomy")
    public AutonomySettings mine() {
        return autonomy.mine();
    }

    @GetMapping("/api/caregivers/me/patients/{patientId}/autonomy")
    public AutonomySettings represented(@PathVariable UUID patientId) {
        return autonomy.represented(patientId);
    }

    @PutMapping("/api/caregivers/me/patients/{patientId}/autonomy")
    public AutonomySettings configure(@PathVariable UUID patientId, @Valid @RequestBody AutonomySettings request) {
        return autonomy.configure(patientId, request);
    }
}
