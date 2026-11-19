package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.audit.AccessLogPageResponse;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.audit.AccessLogQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequestMapping("/api/patients/me/access-log")
@RequiredArgsConstructor
public class AccessLogController {

    private final CurrentPatient currentPatient;
    private final AccessLogQueryService service;

    @GetMapping
    public AccessLogPageResponse list(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.forPatient(currentPatient.get().getId(), from, to, page, size);
    }
}
