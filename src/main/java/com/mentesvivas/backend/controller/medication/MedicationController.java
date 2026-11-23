package com.mentesvivas.backend.controller.medication;

import com.mentesvivas.backend.dto.medication.AdjustStockRequest;
import com.mentesvivas.backend.dto.medication.CreateMedicationRequest;
import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.service.medication.MedicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/medications")
@RequiredArgsConstructor
public class MedicationController {

    private final MedicationService service;

    @GetMapping
    public List<MedicationResponse> list(@PathVariable UUID patientId,
                                         @RequestParam(defaultValue = "false") boolean includeInactive) {
        return service.list(patientId, includeInactive);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationResponse create(@PathVariable UUID patientId, @Valid @RequestBody CreateMedicationRequest body) {
        return service.create(patientId, body);
    }

    @PutMapping("/{medicationId}/stock")
    public MedicationResponse adjustStock(@PathVariable UUID patientId, @PathVariable UUID medicationId,
                                          @Valid @RequestBody AdjustStockRequest body) {
        return service.adjustStock(patientId, medicationId, body);
    }

    @PostMapping("/{medicationId}/deactivate")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivate(@PathVariable UUID patientId, @PathVariable UUID medicationId) {
        service.deactivate(patientId, medicationId);
    }
}
