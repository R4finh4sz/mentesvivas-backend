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
@Table(name = "agitation_protocols")
@Getter
@NoArgsConstructor
public class AgitationProtocol {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private OccurrenceSeverity severity;

    @Column(nullable = false, length = 4000)
    private String instructions;

    @Column(nullable = false)
    private UUID approvedBy;

    @Column(nullable = false)
    private Instant approvedAt;

    public AgitationProtocol(OccurrenceSeverity severity, String instructions, UUID approvedBy, Instant approvedAt) {
        this.severity = severity;
        this.instructions = instructions;
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
    }
}
