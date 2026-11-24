package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.config.PrescriptionProperties;
import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.dto.prescription.PrescriptionResponse;
import com.mentesvivas.backend.dto.prescription.SignPrescriptionRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.prescription.Prescription;
import com.mentesvivas.backend.entity.prescription.PrescriptionDocument;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import com.mentesvivas.backend.entity.prescription.PrescriptionStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import com.mentesvivas.backend.exception.patient.IncompletePrescriptionException;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionDocumentRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionItemRepository;
import com.mentesvivas.backend.repository.prescription.PrescriptionRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.auth.Reauthentication;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PrescriptionSigningService {

    private static final int CODE_BYTES = 16;

    private final PrescriptionDraftService drafts;
    private final DoctorPatientAccess doctorAccess;
    private final PrescriptionRepository repository;
    private final PrescriptionItemRepository itemRepository;
    private final PrescriptionDocumentRepository documentRepository;
    private final AllergyRepository allergyRepository;
    private final UserRepository userRepository;
    private final PrescriptionCompleteness completeness;
    private final PrescriptionItemGuard itemGuard;
    private final Reauthentication reauthentication;
    private final DocumentSigner signer;
    private final PrescriptionPdfRenderer pdfRenderer;
    private final PrescriptionViewFactory viewFactory;
    private final PrescriptionProperties properties;
    private final NotificationService notifications;
    private final AuditTrail auditTrail;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    @Transactional
    public TwoFactorChallengeResponse challenge(UUID prescriptionId) {
        Prescription prescription = drafts.ownedBy(prescriptionId);
        if (!prescription.isDraft()) {
            throw new ConflictException("Esta receita ja foi assinada.");
        }
        return reauthentication.challenge(doctorAccess.requireDoctor());
    }

    @Transactional(noRollbackFor = ServiceUnavailableException.class)
    public PrescriptionResponse sign(UUID prescriptionId, SignPrescriptionRequest request) {
        Prescription prescription = drafts.ownedBy(prescriptionId);
        if (!prescription.isDraft()) {
            return viewFactory.build(prescription);
        }
        User doctor = doctorAccess.requireDoctor();
        List<PrescriptionItem> items = itemRepository.findByPrescriptionIdOrderByPositionAsc(prescription.getId());
        List<String> missing = completeness.missingFields(prescription, items);
        if (!missing.isEmpty()) {
            throw new IncompletePrescriptionException(missing);
        }
        reauthentication.verify(doctor, request.password(), request.code());
        recheckAllergies(prescription, items);

        String code = HexFormat.of().formatHex(randomBytes());
        SignatureResult result = signOrKeepDraft(prescription, doctor, items, code);

        Instant now = clock.instant();
        prescription.setVerificationCode(code);
        prescription.setSignatureReference(result.reference());
        prescription.setSignatureSimulated(result.simulated());
        prescription.setSignedAt(now);
        prescription.setValidUntil(now.plus(Duration.ofDays(properties.validityDays())));
        prescription.setStatus(PrescriptionStatus.SIGNED);
        prescription.setLastSignatureError(null);
        prescription.setUpdatedAt(now);
        documentRepository.save(new PrescriptionDocument(prescription.getId(), render(prescription, doctor, items)));
        markPreviousAsReplaced(prescription);
        auditTrail.record(AuditRecord.of(AuditEventType.PRESCRIPTION_SIGNED, doctor.getId())
                .forPatient(prescription.getPatientId()).withValues(null, prescription.getId().toString()));
        deliver(prescription);
        return viewFactory.build(repository.save(prescription));
    }

    private SignatureResult signOrKeepDraft(Prescription prescription, User doctor, List<PrescriptionItem> items,
                                            String code) {
        try {
            return signer.sign(canonicalContent(prescription, items, code), doctor);
        } catch (ServiceUnavailableException ex) {
            prescription.setLastSignatureError(ex.getMessage());
            repository.save(prescription);
            throw ex;
        }
    }

    private void recheckAllergies(Prescription prescription, List<PrescriptionItem> items) {
        List<Allergy> allergies = allergyRepository.findByPatientIdOrderByCreatedAtAsc(prescription.getPatientId());
        items.forEach(item -> itemGuard.check(item.getMedicationName(), item.getAllergyOverrideJustification(),
                true, allergies));
    }

    private byte[] render(Prescription prescription, User doctor, List<PrescriptionItem> items) {
        User patient = userRepository.findById(prescription.getPatientId()).orElseThrow();
        return pdfRenderer.render(new PrescriptionPdfData(prescription.getKind(), patient.getFullName(),
                doctor.getFullName(), "CRM %s/%s".formatted(doctor.getCrm(), doctor.getCrmState()),
                prescription.getSignedAt(), prescription.getValidUntil(), prescription.getOrientations(), items,
                viewFactory.verificationUrl(prescription), prescription.getSignatureReference(),
                prescription.isSignatureSimulated()));
    }

    private void markPreviousAsReplaced(Prescription prescription) {
        if (prescription.getReplacesId() != null) {
            repository.findById(prescription.getReplacesId())
                    .ifPresent(previous -> previous.setReplacedById(prescription.getId()));
        }
    }

    private void deliver(Prescription prescription) {
        try {
            notifications.notify(prescription.getPatientId(), NotificationType.PRESCRIPTION_AVAILABLE,
                    prescription.getId(), "Um novo documento medico esta disponivel na sua area de documentos.");
        } catch (RuntimeException ex) {
            log.warn("Could not notify the patient about prescription {}; the document stays available",
                    prescription.getId(), ex);
        }
    }

    private byte[] canonicalContent(Prescription prescription, List<PrescriptionItem> items, String code) {
        StringBuilder content = new StringBuilder()
                .append(prescription.getPatientId()).append('|').append(prescription.getDoctorId()).append('|')
                .append(prescription.getKind()).append('|').append(prescription.getOrientations()).append('|')
                .append(code);
        items.forEach(item -> content.append('|').append(item.getMedicationName()).append(';')
                .append(item.getPresentation()).append(';').append(item.getDose()).append(';')
                .append(item.getRoute()).append(';').append(item.getFrequency()).append(';')
                .append(item.getDuration()).append(';').append(item.getQuantity()));
        return content.toString().getBytes(StandardCharsets.UTF_8);
    }

    private byte[] randomBytes() {
        byte[] bytes = new byte[CODE_BYTES];
        random.nextBytes(bytes);
        return bytes;
    }
}
