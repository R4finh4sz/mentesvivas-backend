package com.mentesvivas.backend.repository.prescription;

import com.mentesvivas.backend.entity.prescription.PrescriptionDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PrescriptionDocumentRepository extends JpaRepository<PrescriptionDocument, UUID> {
}
