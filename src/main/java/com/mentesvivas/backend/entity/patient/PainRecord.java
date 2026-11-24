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
@Table(name = "pain_records")
@Getter
@NoArgsConstructor
public class PainRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PainScale scale;

    @Column(nullable = false)
    private int score;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    public PainRecord(UUID patientId, PainScale scale, int score, UUID recordedBy, Instant recordedAt) {
        this.patientId = patientId;
        this.scale = scale;
        this.score = score;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
    }
}
