package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.HydrationResponse;
import com.mentesvivas.backend.service.patient.HydrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/hydration")
@RequiredArgsConstructor
public class HydrationController {

    private final HydrationService service;

    @GetMapping
    public HydrationResponse today(@PathVariable UUID patientId) {
        return service.today(patientId);
    }
}
