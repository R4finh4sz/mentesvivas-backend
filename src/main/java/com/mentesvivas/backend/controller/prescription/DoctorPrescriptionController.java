package com.mentesvivas.backend.controller.prescription;

import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.dto.prescription.ReplacePrescriptionRequest;
import com.mentesvivas.backend.dto.prescription.SavePrescriptionRequest;
import com.mentesvivas.backend.dto.prescription.SignPrescriptionRequest;
import com.mentesvivas.backend.service.prescription.DoctorPrescriptionPdfService;
import com.mentesvivas.backend.service.prescription.PrescriptionDraftService;
import com.mentesvivas.backend.service.prescription.PrescriptionFile;
import com.mentesvivas.backend.service.prescription.PrescriptionSigningService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/me")
@RequiredArgsConstructor
public class DoctorPrescriptionController {

    private final PrescriptionDraftService drafts;
    private final PrescriptionSigningService signing;
    private final DoctorPrescriptionPdfService pdfService;

    @PostMapping("/patients/{patientId}/prescriptions")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse create(@PathVariable UUID patientId, @Valid @RequestBody SavePrescriptionRequest body) {
        return drafts.create(patientId, body);
    }

    @GetMapping("/patients/{patientId}/prescriptions")
    public List<PrescriptionResponse> list(@PathVariable UUID patientId) {
        return drafts.list(patientId);
    }

    @GetMapping("/prescriptions/{id}")
    public PrescriptionResponse get(@PathVariable UUID id) {
        return drafts.get(id);
    }

    @PutMapping("/prescriptions/{id}")
    public PrescriptionResponse update(@PathVariable UUID id, @Valid @RequestBody SavePrescriptionRequest body) {
        return drafts.update(id, body);
    }

    @PostMapping("/prescriptions/{id}/signature-challenge")
    public TwoFactorChallengeResponse challenge(@PathVariable UUID id) {
        return signing.challenge(id);
    }

    @PostMapping("/prescriptions/{id}/sign")
    public PrescriptionResponse sign(@PathVariable UUID id, @Valid @RequestBody SignPrescriptionRequest body) {
        return signing.sign(id, body);
    }

    @PostMapping("/prescriptions/{id}/replace")
    @ResponseStatus(HttpStatus.CREATED)
    public PrescriptionResponse replace(@PathVariable UUID id, @Valid @RequestBody ReplacePrescriptionRequest body) {
        return drafts.replace(id, body);
    }

    @GetMapping("/prescriptions/{id}/pdf")
    public ResponseEntity<byte[]> pdf(@PathVariable UUID id) {
        return asPdf(pdfService.pdf(id));
    }

    static ResponseEntity<byte[]> asPdf(PrescriptionFile file) {
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + file.fileName() + "\"")
                .body(file.content());
    }
}
