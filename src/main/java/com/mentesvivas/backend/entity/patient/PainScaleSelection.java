package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pain_scale_selections")
@Getter
@NoArgsConstructor
public class PainScaleSelection {

    @Id
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PainScale scale;

    @Column(nullable = false)
    private UUID selectedBy;

    @Column(nullable = false)
    private Instant selectedAt;

    public PainScaleSelection(UUID patientId) {
        this.patientId = patientId;
    }

    public void select(PainScale scale, UUID doctorId, Instant now) {
        this.scale = scale;
        this.selectedBy = doctorId;
        this.selectedAt = now;
    }
}
