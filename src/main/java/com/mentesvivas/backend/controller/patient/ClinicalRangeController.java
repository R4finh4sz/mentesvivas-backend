package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.ClinicalRangePolicyResponse;
import com.mentesvivas.backend.dto.patient.PublishClinicalRangesRequest;
import com.mentesvivas.backend.service.patient.ClinicalRangeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ClinicalRangeController {

    private final ClinicalRangeService ranges;

    @PostMapping("/api/doctors/me/vital-sign-ranges")
    @ResponseStatus(HttpStatus.CREATED)
    public ClinicalRangePolicyResponse publish(@Valid @RequestBody PublishClinicalRangesRequest request) {
        return ranges.publish(request);
    }

    @GetMapping("/api/clinical/vital-sign-ranges")
    public ClinicalRangePolicyResponse active() {
        return ranges.active();
    }
}
