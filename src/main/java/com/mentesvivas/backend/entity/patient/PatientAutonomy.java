package com.mentesvivas.backend.entity.patient;

import com.mentesvivas.backend.dto.patient.AutonomySettings;
import com.mentesvivas.backend.dto.patient.AutonomySettings.Mode;
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
@Table(name = "patient_autonomy")
@NoArgsConstructor
public class PatientAutonomy {

    @Id
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 12)
    private Mode mode;

    @Column(nullable = false)
    private boolean restrictCareRecords;

    @Column(nullable = false)
    private boolean restrictGames;

    @Getter
    @Column(nullable = false)
    private UUID configuredBy;

    @Getter
    @Column(nullable = false)
    private Instant configuredAt;

    public PatientAutonomy(UUID patientId, AutonomySettings settings, UUID configuredBy, Instant configuredAt) {
        this.patientId = patientId;
        this.mode = settings.mode();
        this.restrictCareRecords = settings.restrictCareRecords();
        this.restrictGames = settings.restrictGames();
        this.configuredBy = configuredBy;
        this.configuredAt = configuredAt;
    }

    public AutonomySettings view() {
        return new AutonomySettings(mode, restrictCareRecords, restrictGames);
    }
}
