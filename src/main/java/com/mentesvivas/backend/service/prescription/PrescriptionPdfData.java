package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;

import java.time.Instant;
import java.util.List;

public record PrescriptionPdfData(
        DocumentKind kind,
        String patientName,
        String doctorName,
        String doctorRegistry,
        Instant issuedAt,
        Instant validUntil,
        String orientations,
        List<PrescriptionItem> items,
        String verificationUrl,
        String signatureReference,
        boolean simulated
) {
}
