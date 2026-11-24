package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "diagnosis_revisions")
@Getter
@NoArgsConstructor
public class DiagnosisRevision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID recordedBy;

    @Column(nullable = false)
    private Instant recordedAt;

    @Column(nullable = false, length = 300)
    private String disease;

    @Column(length = 5)
    private String primaryCid;

    @ElementCollection
    @CollectionTable(name = "diagnosis_secondary_codes", joinColumns = @JoinColumn(name = "revision_id"))
    @OrderColumn(name = "position")
    @Column(name = "code", nullable = false, length = 5)
    private List<String> secondaryCids;

    public DiagnosisRevision(UUID patientId, UUID recordedBy, Instant recordedAt, String disease,
                             String primaryCid, List<String> secondaryCids) {
        this.patientId = patientId;
        this.recordedBy = recordedBy;
        this.recordedAt = recordedAt;
        this.disease = disease;
        this.primaryCid = primaryCid;
        this.secondaryCids = new ArrayList<>(secondaryCids);
    }
}
