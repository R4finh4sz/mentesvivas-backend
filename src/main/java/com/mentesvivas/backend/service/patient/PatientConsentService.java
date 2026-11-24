package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.PatientConsentResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.patient.ConsentScope;
import com.mentesvivas.backend.entity.patient.PatientConsent;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.patient.PatientConsentRepository;
import com.mentesvivas.backend.repository.patient.GeofenceRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatientConsentService {

    private final PatientConsentRepository repository;
    private final GeofenceRepository fences;
    private final CaregiverPatientAccess caregivers;
    private final EntityManager entityManager;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<PatientConsentResponse> listOwn(UUID patientId) {
        return list(patientId);
    }

    @Transactional(readOnly = true)
    public List<PatientConsentResponse> listRepresented(UUID patientId) {
        caregivers.requireRepresentativeOf(patientId);
        return list(patientId);
    }

    @Transactional
    public PatientConsentResponse decideOwn(UUID patientId, ConsentScope scope, boolean granted) {
        return decide(patientId, patientId, scope, granted);
    }

    @Transactional
    public PatientConsentResponse decideRepresented(UUID patientId, ConsentScope scope, boolean granted) {
        UUID caregiverId = caregivers.requireRepresentativeOf(patientId).getId();
        return decide(patientId, caregiverId, scope, granted);
    }

    public boolean granted(UUID patientId, ConsentScope scope) {
        return repository.findByPatientIdAndScope(patientId, scope).map(PatientConsent::isGranted).orElse(false);
    }

    public void require(UUID patientId, ConsentScope scope) {
        if (!granted(patientId, scope)) {
            throw new AccessDeniedException("O consentimento para este uso precisa estar ativo.");
        }
    }

    public void lockForCollection(UUID patientId) {
        lock(patientId, LockModeType.PESSIMISTIC_WRITE);
    }

    private PatientConsentResponse decide(UUID patientId, UUID actorId, ConsentScope scope, boolean granted) {
        lock(patientId, LockModeType.PESSIMISTIC_WRITE);
        PatientConsent consent = repository.findByPatientIdAndScope(patientId, scope)
                .orElseGet(() -> new PatientConsent(patientId, scope));
        UUID previousActor = consent.getDecidedBy();
        boolean ownDecision = actorId.equals(patientId);
        if (!ownDecision && patientId.equals(previousActor)) {
            if (consent.isGranted() == granted) {
                return view(consent);
            }
            throw new ConflictException("A decisão do paciente prevalece.");
        }
        if (actorId.equals(previousActor) && consent.isGranted() == granted) {
            return view(consent);
        }
        String before = previousActor == null ? null : scope + ":" + consent.isGranted();
        consent.decide(granted, actorId, clock.instant());
        repository.save(consent);
        AuditRecord record = AuditRecord.of(AuditEventType.PATIENT_CONSENT_CHANGED, actorId)
                .forPatient(patientId).withValues(before, scope + ":" + granted);
        audit.record(ownDecision ? record : record.onBehalfOf(patientId));
        if (scope == ConsentScope.GEOLOCATION && !granted) {
            fences.findById(patientId).ifPresent(zone -> {
                fences.delete(zone);
                AuditRecord disabled = AuditRecord.of(AuditEventType.GEOFENCE_CHANGED, actorId)
                        .forPatient(patientId).withValues("ACTIVE", "DISABLED");
                audit.record(ownDecision ? disabled : disabled.onBehalfOf(patientId));
            });
        }
        return view(consent);
    }

    private List<PatientConsentResponse> list(UUID patientId) {
        Map<ConsentScope, PatientConsent> decisions = repository.findByPatientId(patientId).stream()
                .collect(Collectors.toMap(PatientConsent::getScope, Function.identity()));
        return Arrays.stream(ConsentScope.values()).map(scope -> {
            PatientConsent decision = decisions.get(scope);
            return decision == null ? new PatientConsentResponse(scope, false, false, null) : view(decision);
        }).toList();
    }

    private void lock(UUID patientId, LockModeType mode) {
        if (entityManager.find(User.class, patientId, mode) == null) {
            throw new ResourceNotFoundException("Paciente não encontrado.");
        }
    }

    private static PatientConsentResponse view(PatientConsent consent) {
        return new PatientConsentResponse(consent.getScope(), consent.isGranted(),
                consent.getPatientId().equals(consent.getDecidedBy()), consent.getDecidedAt());
    }
}
