package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.dto.clinical.ClinicalSummaryResponse;
import com.mentesvivas.backend.dto.clinical.TimelineResponse;
import com.mentesvivas.backend.service.clinical.ClinicalSummaryService;
import com.mentesvivas.backend.service.clinical.ClinicalTimelineService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/me/patients/{patientId}")
@RequiredArgsConstructor
public class ClinicalRecordController {

    private final ClinicalSummaryService summaryService;
    private final ClinicalTimelineService timelineService;

    @GetMapping("/summary")
    public ClinicalSummaryResponse summary(@PathVariable UUID patientId) {
        return summaryService.summarize(patientId);
    }

    @GetMapping("/timeline")
    public TimelineResponse timeline(@PathVariable UUID patientId,
                                     @RequestParam(required = false) Instant from,
                                     @RequestParam(required = false) Instant to) {
        return timelineService.timeline(patientId, from, to);
    }
}
