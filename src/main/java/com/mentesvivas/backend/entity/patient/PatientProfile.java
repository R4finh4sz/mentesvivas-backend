package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "patient_profiles")
@Getter
@Setter
@NoArgsConstructor
public class PatientProfile {

    @Id
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(length = 15)
    private BloodType bloodType;

    @Column(length = 300)
    private String declaredDisease;

    @Column(nullable = false)
    private boolean declaredDiseaseValidated;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AllergyStatus allergyStatus = AllergyStatus.NOT_INFORMED;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public PatientProfile(UUID userId) {
        this.userId = userId;
    }

    @PrePersist
    void prePersist() {
        createdAt = Instant.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
