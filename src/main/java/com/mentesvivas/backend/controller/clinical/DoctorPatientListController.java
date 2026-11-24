package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.dto.clinical.DoctorPatientListItem;
import com.mentesvivas.backend.service.clinical.DoctorPatientListService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class DoctorPatientListController {

    private final DoctorPatientListService patients;

    @GetMapping("/api/doctors/me/patients")
    public List<DoctorPatientListItem> list(@RequestParam(required = false) Boolean pendingAlert,
                                            @RequestParam(required = false) Instant activeSince,
                                            @RequestParam(defaultValue = "0") @Min(0) int page,
                                            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        return patients.list(pendingAlert, activeSince, page, size);
    }
}
