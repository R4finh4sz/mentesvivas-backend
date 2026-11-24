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
@Table(name = "clinical_range_policies")
@Getter
@NoArgsConstructor
public class ClinicalRangePolicy {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID approvedBy;

    @Column(nullable = false)
    private Instant approvedAt;

    @ElementCollection
    @CollectionTable(name = "clinical_ranges", joinColumns = @JoinColumn(name = "policy_id"))
    @OrderColumn(name = "position")
    private List<ClinicalRange> ranges;

    public ClinicalRangePolicy(UUID approvedBy, Instant approvedAt, List<ClinicalRange> ranges) {
        this.approvedBy = approvedBy;
        this.approvedAt = approvedAt;
        this.ranges = new ArrayList<>(ranges);
    }
}
