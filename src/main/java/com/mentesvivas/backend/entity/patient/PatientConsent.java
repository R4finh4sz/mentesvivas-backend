package com.mentesvivas.backend.entity.patient;

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

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_consents")
@Getter
@NoArgsConstructor
public class PatientConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ConsentScope scope;

    @Column(nullable = false)
    private boolean granted;

    @Column(nullable = false)
    private UUID decidedBy;

    @Column(nullable = false)
    private Instant decidedAt;

    public PatientConsent(UUID patientId, ConsentScope scope) {
        this.patientId = patientId;
        this.scope = scope;
    }

    public void decide(boolean granted, UUID actor, Instant now) {
        this.granted = granted;
        this.decidedBy = actor;
        this.decidedAt = now;
    }
}
