package com.mentesvivas.backend.controller.medication;

import com.mentesvivas.backend.dto.medication.CorrectDoseRequest;
import com.mentesvivas.backend.dto.medication.DoseResponse;
import com.mentesvivas.backend.dto.medication.RecordDoseRequest;
import com.mentesvivas.backend.service.medication.DoseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}")
@RequiredArgsConstructor
public class DoseController {

    private final DoseService service;

    @PostMapping("/medications/{medicationId}/doses")
    @ResponseStatus(HttpStatus.CREATED)
    public DoseResponse record(@PathVariable UUID patientId, @PathVariable UUID medicationId,
                               @Valid @RequestBody RecordDoseRequest body) {
        return service.record(patientId, medicationId, body);
    }

    @PostMapping("/doses/{doseId}/corrections")
    public DoseResponse correct(@PathVariable UUID patientId, @PathVariable UUID doseId,
                                @Valid @RequestBody CorrectDoseRequest body) {
        return service.correct(patientId, doseId, body);
    }

    @GetMapping("/doses")
    public List<DoseResponse> list(@PathVariable UUID patientId,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
                                   @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.list(patientId, from, to);
    }
}
