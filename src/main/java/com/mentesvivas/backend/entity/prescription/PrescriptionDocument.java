package com.mentesvivas.backend.entity.prescription;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "prescription_documents")
@Getter
@NoArgsConstructor
public class PrescriptionDocument {

    @Id
    private UUID prescriptionId;

    @Column(nullable = false)
    private byte[] content;

    public PrescriptionDocument(UUID prescriptionId, byte[] content) {
        this.prescriptionId = prescriptionId;
        this.content = content;
    }
}
