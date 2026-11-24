package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.AutonomySettings;
import com.mentesvivas.backend.dto.patient.AutonomySettings.Mode;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.patient.PatientAutonomy;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.patient.PatientAutonomyRepository;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.notification.NotificationService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class PatientAutonomyService {

    private final PatientAutonomyRepository repository;
    private final CaregiverPatientLinkRepository links;
    private final CurrentPatient patient;
    private final CaregiverPatientAccess caregivers;
    private final EntityManager entityManager;
    private final AuditTrail audit;
    private final NotificationService notifications;
    private final Clock clock;

    @Transactional(readOnly = true)
    public AutonomySettings mine() {
        return settings(patient.get().getId());
    }

    @Transactional(readOnly = true)
    public AutonomySettings represented(UUID patientId) {
        caregivers.requireRepresentativeOf(patientId);
        return settings(patientId);
    }

    @Transactional
    public AutonomySettings configure(UUID patientId, AutonomySettings request) {
        UUID caregiverId = caregivers.requireRepresentativeOf(patientId).getId();
        if (request.mode() == Mode.AUTONOMOUS && (request.restrictCareRecords() || request.restrictGames())) {
            throw new ImpossibleValueException("As restrições exigem o modo assistido.");
        }
        entityManager.find(User.class, patientId, LockModeType.PESSIMISTIC_WRITE);
        AutonomySettings previous = settings(patientId);
        if (!previous.equals(request)) {
            repository.save(new PatientAutonomy(patientId, request, caregiverId, clock.instant()));
            audit.record(AuditRecord.of(AuditEventType.PATIENT_AUTONOMY_CHANGED, caregiverId)
                    .forPatient(patientId).onBehalfOf(patientId).withValues(previous.toString(), request.toString()));
            notifications.notify(patientId, NotificationType.AUTONOMY_CHANGED, patientId,
                    "Seu modo de autonomia foi atualizado. Consulte as permissões atuais.");
        }
        return request;
    }

    public AutonomySettings settings(UUID patientId) {
        return repository.findById(patientId).filter(saved -> links
                        .findByCaregiverIdAndPatientId(saved.getConfiguredBy(), patientId)
                        .filter(link -> link.getStatus() == LinkStatus.ACTIVE)
                        .filter(link -> !link.getUpdatedAt().isAfter(saved.getConfiguredAt()))
                        .isPresent())
                .map(PatientAutonomy::view).orElseGet(AutonomySettings::defaults);
    }
}
