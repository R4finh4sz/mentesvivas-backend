package com.mentesvivas.backend.controller.prescription;

import com.mentesvivas.backend.dto.medication.MedicationResponse;
import com.mentesvivas.backend.dto.prescription.AddToRoutineRequest;
import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.service.prescription.PatientPrescriptionService;
import com.mentesvivas.backend.service.prescription.PrescriptionRoutineService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/prescriptions")
@RequiredArgsConstructor
public class PatientPrescriptionController {

    private final PatientPrescriptionService service;
    private final PrescriptionRoutineService routineService;

    @GetMapping
    public List<PrescriptionResponse> list(@PathVariable UUID patientId) {
        return service.list(patientId);
    }

    @GetMapping("/{id}")
    public PrescriptionResponse get(@PathVariable UUID patientId, @PathVariable UUID id) {
        return service.get(patientId, id);
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID patientId, @PathVariable UUID id) {
        return DoctorPrescriptionController.asPdf(service.pdf(patientId, id));
    }

    @PostMapping("/{id}/items/{itemId}/routine")
    @ResponseStatus(HttpStatus.CREATED)
    public MedicationResponse addToRoutine(@PathVariable UUID patientId, @PathVariable UUID id,
                                           @PathVariable UUID itemId, @Valid @RequestBody AddToRoutineRequest body) {
        return routineService.addToRoutine(patientId, id, itemId, body);
    }
}
