package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.OccurrenceResponse;
import com.mentesvivas.backend.dto.patient.RecordOccurrenceRequest;
import com.mentesvivas.backend.entity.patient.OccurrenceType;
import com.mentesvivas.backend.service.patient.OccurrenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/occurrences")
@RequiredArgsConstructor
public class OccurrenceController {

    private final OccurrenceService service;

    @GetMapping
    public List<OccurrenceResponse> history(@PathVariable UUID patientId,
                                            @RequestParam(required = false) OccurrenceType type,
                                            @RequestParam(required = false) String keyword) {
        return service.history(patientId, type, keyword);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OccurrenceResponse record(@PathVariable UUID patientId, @Valid @RequestBody RecordOccurrenceRequest body) {
        return service.record(patientId, body);
    }
}
