package com.mentesvivas.backend.dto.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PrescriptionResponse(
        UUID id,
        UUID patientId,
        String patientName,
        UUID doctorId,
        String doctorName,
        String doctorRegistry,
        DocumentKind kind,
        PrescriptionStatus status,
        String orientations,
        List<PrescriptionItemResponse> items,
        List<String> missingFields,
        List<PrescriptionWarning> warnings,
        Instant createdAt,
        Instant signedAt,
        Instant validUntil,
        boolean expired,
        boolean replaced,
        UUID replacesId,
        String replacementJustification,
        UUID replacedById,
        String verificationCode,
        String verificationUrl,
        boolean signatureSimulated,
        String lastSignatureError
) {
}
