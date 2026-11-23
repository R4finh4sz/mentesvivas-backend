package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionDocument;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.PrescriptionDocumentRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientPrescriptionService {

    private final PatientCareAccess access;
    private final PrescriptionRepository repository;
    private final PrescriptionDocumentRepository documentRepository;
    private final PrescriptionViewFactory viewFactory;
    private final AccessLog accessLog;

    @Transactional
    public List<PrescriptionResponse> list(UUID patientId) {
        User reader = access.requireWriter(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.PRESCRIPTIONS);
        return repository.findByPatientIdAndStatusOrderBySignedAtDesc(patientId, PrescriptionStatus.SIGNED).stream()
                .map(viewFactory::build)
                .toList();
    }

    @Transactional
    public PrescriptionResponse get(UUID patientId, UUID prescriptionId) {
        User reader = access.requireWriter(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.PRESCRIPTIONS);
        return viewFactory.build(find(patientId, prescriptionId));
    }

    @Transactional
    public PrescriptionFile pdf(UUID patientId, UUID prescriptionId) {
        User reader = access.requireWriter(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.PRESCRIPTIONS);
        Prescription prescription = find(patientId, prescriptionId);
        PrescriptionDocument document = documentRepository.findById(prescription.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Documento nao encontrado."));
        return new PrescriptionFile("receita-%s.pdf".formatted(prescription.getId()), document.getContent());
    }

    private Prescription find(UUID patientId, UUID prescriptionId) {
        return repository.findByIdAndPatientIdAndStatus(prescriptionId, patientId, PrescriptionStatus.SIGNED)
                .orElseThrow(() -> new ResourceNotFoundException("Receita nao encontrada."));
    }
}
