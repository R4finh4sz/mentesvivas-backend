package com.mentesvivas.backend.entity.link;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
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
@Table(name = "doctor_patient_links")
@Getter
@Setter
@NoArgsConstructor
public class DoctorPatientLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID doctorId;

    @Column(nullable = false)
    private UUID patientId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LinkStatus status;

    @Column(nullable = false)
    private boolean doctorConfirmed;

    @Column(nullable = false)
    private boolean patientAuthorized;

    private UUID initiatedBy;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    public DoctorPatientLink(UUID doctorId, UUID patientId, LinkStatus status, Instant createdAt) {
        this.doctorId = doctorId;
        this.patientId = patientId;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.doctorConfirmed = status == LinkStatus.ACTIVE;
        this.patientAuthorized = status == LinkStatus.ACTIVE;
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        updatedAt = createdAt;
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }
}
