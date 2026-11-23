package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.dto.clinical.ClinicalSummaryResponse;
import com.mentesvivas.backend.service.clinical.ClinicalSummaryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/me/patients/{patientId}")
@RequiredArgsConstructor
public class ClinicalRecordController {

    private final ClinicalSummaryService summaryService;

    @GetMapping("/summary")
    public ClinicalSummaryResponse summary(@PathVariable UUID patientId) {
        return summaryService.summarize(patientId);
    }
}
