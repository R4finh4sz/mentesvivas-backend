package com.mentesvivas.backend.controller.link;

import com.mentesvivas.backend.dto.audit.AccessLogPageResponse;
import com.mentesvivas.backend.dto.terms.AcceptTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsAcceptanceResponse;
import com.mentesvivas.backend.service.audit.AccessLogQueryService;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.terms.TermsAcceptanceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
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
@RequestMapping("/api/caregivers/me/patients/{patientId}")
@RequiredArgsConstructor
public class RepresentativeController {

    private final TermsAcceptanceService termsAcceptanceService;
    private final AccessLogQueryService accessLogQueryService;
    private final CaregiverPatientAccess caregiverAccess;

    @PostMapping("/terms/accept")
    public TermsAcceptanceResponse acceptTerms(@PathVariable UUID patientId, @Valid @RequestBody AcceptTermsRequest body) {
        return termsAcceptanceService.acceptOnBehalfOf(patientId, body);
    }

    @GetMapping("/access-log")
    public AccessLogPageResponse accessLog(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        caregiverAccess.requireRepresentativeOf(patientId);
        return accessLogQueryService.forPatient(patientId, from, to, page, size);
    }
}
