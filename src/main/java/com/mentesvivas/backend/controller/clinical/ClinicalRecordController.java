package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.dto.clinical.ClinicalSummaryResponse;
import com.mentesvivas.backend.dto.clinical.ExportTimelineRequest;
import com.mentesvivas.backend.dto.clinical.TimelineResponse;
import com.mentesvivas.backend.dto.game.GamePerformanceReport;
import com.mentesvivas.backend.service.clinical.ClinicalSummaryService;
import com.mentesvivas.backend.service.clinical.ClinicalTimelineExportService;
import com.mentesvivas.backend.service.clinical.ClinicalTimelineService;
import com.mentesvivas.backend.service.game.GamePerformanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
    private final GamePerformanceService gamePerformance;
    private final ClinicalTimelineExportService exports;

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

    @GetMapping("/game-performance")
    public GamePerformanceReport gamePerformance(@PathVariable UUID patientId) {
        return gamePerformance.report(patientId);
    }

    @PostMapping("/timeline/export-challenge")
    public TwoFactorChallengeResponse exportChallenge(@PathVariable UUID patientId) {
        return exports.challenge(patientId);
    }

    @PostMapping(value = "/timeline/export", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> export(@PathVariable UUID patientId,
                                         @Valid @RequestBody ExportTimelineRequest request) {
        return ResponseEntity.ok().contentType(MediaType.APPLICATION_PDF).cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"linha-do-tempo.pdf\"")
                .body(exports.export(patientId, request));
    }
}
