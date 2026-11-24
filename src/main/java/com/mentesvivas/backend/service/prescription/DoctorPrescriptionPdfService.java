package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.PrescriptionDocumentRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorPrescriptionPdfService {

    private final PrescriptionDraftService drafts;
    private final PrescriptionDocumentRepository documentRepository;
    private final AccessLog accessLog;

    @Transactional
    public PrescriptionFile pdf(UUID prescriptionId) {
        Prescription prescription = drafts.ownedBy(prescriptionId);
        if (prescription.isDraft()) {
            throw new ConflictException("O PDF so existe depois da assinatura.");
        }
        accessLog.record(prescription.getDoctorId(), prescription.getPatientId(), PatientDataArea.PRESCRIPTIONS);
        byte[] content = documentRepository.findById(prescription.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado.")).getContent();
        return new PrescriptionFile("receita-%s.pdf".formatted(prescription.getId()), content);
    }
}
