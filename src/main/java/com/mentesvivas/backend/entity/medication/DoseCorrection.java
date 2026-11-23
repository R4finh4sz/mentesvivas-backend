package com.mentesvivas.backend.entity.medication;

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
@Table(name = "dose_corrections")
@Getter
@NoArgsConstructor
public class DoseCorrection {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID doseId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DoseStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private DoseStatus newStatus;

    @Column(nullable = false, length = 300)
    private String justification;

    @Column(nullable = false)
    private UUID correctedBy;

    @Column(nullable = false)
    private Instant correctedAt;

    public DoseCorrection(UUID doseId, DoseStatus previousStatus, DoseStatus newStatus, String justification,
                          UUID correctedBy, Instant correctedAt) {
        this.doseId = doseId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
        this.justification = justification;
        this.correctedBy = correctedBy;
        this.correctedAt = correctedAt;
    }
}
