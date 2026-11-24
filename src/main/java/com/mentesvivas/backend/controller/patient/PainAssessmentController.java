package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.PainAssessment;
import com.mentesvivas.backend.entity.patient.PainScale;
import com.mentesvivas.backend.service.patient.PainAssessmentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class PainAssessmentController {

    private final PainAssessmentService service;

    @PutMapping("/api/doctors/me/patients/{patientId}/pain-scale")
    public PainAssessment.Selection select(@PathVariable UUID patientId, @Valid @RequestBody SelectionRequest body) {
        return service.select(patientId, body.scale());
    }

    @GetMapping("/api/care/patients/{patientId}/pain-scale")
    public PainAssessment.Selection selection(@PathVariable UUID patientId) {
        return service.selection(patientId);
    }

    @PostMapping("/api/care/patients/{patientId}/pain-records")
    @ResponseStatus(HttpStatus.CREATED)
    public PainAssessment.Recorded record(@PathVariable UUID patientId, @Valid @RequestBody ScoreRequest body) {
        return service.record(patientId, body.scale(), body.score());
    }

    @GetMapping("/api/care/patients/{patientId}/pain-records")
    public List<PainAssessment.Recorded> history(@PathVariable UUID patientId,
                                                 @RequestParam(defaultValue = "0") @Min(0) int page,
                                                 @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return service.history(patientId, page, size);
    }

    public record SelectionRequest(@NotNull PainScale scale) {}

    public record ScoreRequest(@NotNull PainScale scale, @NotNull Integer score) {}
}
