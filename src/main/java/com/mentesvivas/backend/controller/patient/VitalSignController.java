package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.RecordVitalSignRequest;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;
import com.mentesvivas.backend.service.patient.VitalSignService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/vital-signs")
@RequiredArgsConstructor
public class VitalSignController {

    private final VitalSignService service;

    @GetMapping
    public List<VitalSignResponse> history(@PathVariable UUID patientId) {
        return service.history(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VitalSignResponse record(@PathVariable UUID patientId, @Valid @RequestBody RecordVitalSignRequest body) {
        return service.record(patientId, body);
    }
}
