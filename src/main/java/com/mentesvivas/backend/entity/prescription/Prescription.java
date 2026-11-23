package com.mentesvivas.backend.entity.prescription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
public class Prescription {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID doctorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentKind kind;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PrescriptionStatus status;

    @Column(length = 2000)
    private String orientations;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant signedAt;

    private Instant validUntil;

    @Column(length = 40)
    private String verificationCode;

    @Column(length = 200)
    private String signatureReference;

    @Column(nullable = false)
    private boolean signatureSimulated;

    private UUID replacesId;

    @Column(length = 500)
    private String replacementJustification;

    private UUID replacedById;

    @Column(length = 300)
    private String lastSignatureError;

    private Instant expiryNoticeSentAt;

    public boolean isDraft() {
        return status == PrescriptionStatus.DRAFT;
    }

    public boolean isReplaced() {
        return replacedById != null;
    }

    public boolean isExpiredAt(Instant moment) {
        return validUntil != null && !validUntil.isAfter(moment);
    }
}
