package com.mentesvivas.backend.controller.prescription;

import com.mentesvivas.backend.dto.prescription.VerificationResponse;
import com.mentesvivas.backend.service.prescription.PrescriptionVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public/prescriptions")
@RequiredArgsConstructor
public class PrescriptionVerificationController {

    private final PrescriptionVerificationService service;

    @GetMapping("/{code}")
    public VerificationResponse verify(@PathVariable String code) {
        return service.verify(code);
    }
}
