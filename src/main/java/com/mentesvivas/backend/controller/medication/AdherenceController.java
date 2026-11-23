package com.mentesvivas.backend.controller.medication;

import com.mentesvivas.backend.dto.medication.AdherenceResponse;
import com.mentesvivas.backend.service.medication.AdherenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/adherence")
@RequiredArgsConstructor
public class AdherenceController {

    private final AdherenceService service;

    @GetMapping
    public AdherenceResponse get(@PathVariable UUID patientId) {
        return service.calculate(patientId);
    }
}
