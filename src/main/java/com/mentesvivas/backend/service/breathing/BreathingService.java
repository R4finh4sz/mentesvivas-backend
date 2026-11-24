package com.mentesvivas.backend.service.breathing;

import com.mentesvivas.backend.dto.breathing.BreathingExerciseResponse;
import com.mentesvivas.backend.dto.breathing.CreateBreathingExerciseRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.breathing.BreathingExercise;
import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.breathing.BreathingExerciseRepository;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.notification.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BreathingService {

    private static final String START_NOTICE =
            "Ao iniciar, seus cuidadores com vínculo ativo receberão um aviso silencioso.";

    private final BreathingExerciseRepository exercises;
    private final CaregiverPatientLinkRepository links;
    private final NotificationService notifications;
    private final DoctorPatientAccess doctorAccess;
    private final CurrentUser currentUser;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional
    public BreathingExerciseResponse.Validated validate(CreateBreathingExerciseRequest request) {
        UUID doctorId = doctorAccess.requireDoctor().getId();
        BreathingExercise exercise = exercises.save(new BreathingExercise(request.title().trim(),
                request.inhaleSeconds(), request.holdSeconds(), request.exhaleSeconds(), request.cycles(),
                doctorId, clock.instant()));
        return review(exercise);
    }

    @Transactional(readOnly = true)
    public List<BreathingExerciseResponse.Validated> all() {
        return exercises.findAllByOrderByTitleAscIdAsc().stream().map(BreathingService::review).toList();
    }

    @Transactional
    public BreathingExerciseResponse.Validated changePublication(UUID id, boolean published) {
        BreathingExercise exercise = exercises.findExerciseById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercício não encontrado."));
        boolean previous = exercise.isPublished();
        if (previous != published) {
            exercise.setPublished(published);
            auditTrail.record(AuditRecord.of(AuditEventType.BREATHING_PUBLICATION_CHANGED, currentUser.get().getId())
                    .withValues(id + ":" + previous, id + ":" + published));
        }
        return review(exercise);
    }

    @Transactional(readOnly = true)
    public BreathingExerciseResponse.Catalog catalog() {
        List<BreathingExerciseResponse> published = exercises.findByPublishedTrueOrderByTitleAscIdAsc()
                .stream().map(BreathingService::response).toList();
        return new BreathingExerciseResponse.Catalog(published, START_NOTICE);
    }

    @Transactional
    public BreathingExerciseResponse.Start start(UUID id) {
        UUID patientId = currentUser.get().getId();
        BreathingExercise exercise = exercises.findByIdAndPublishedTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exercício indisponível."));
        List<CaregiverPatientLink> caregivers = links.findByPatientIdAndStatus(patientId, LinkStatus.ACTIVE);
        for (CaregiverPatientLink link : caregivers) {
            notifications.notify(link.getCaregiverId(), NotificationType.BREATHING_STARTED, patientId,
                    "O paciente iniciou um exercício de respiração guiada.");
        }
        String notice = caregivers.isEmpty() ? "Exercício iniciado. Você está sem cuidador com vínculo ativo."
                : "Exercício iniciado. Seus cuidadores receberam um aviso silencioso.";
        return new BreathingExerciseResponse.Start(response(exercise), notice);
    }

    private static BreathingExerciseResponse.Validated review(BreathingExercise exercise) {
        return new BreathingExerciseResponse.Validated(response(exercise), exercise.getValidatedBy(),
                exercise.getValidatedAt(), exercise.isPublished());
    }

    private static BreathingExerciseResponse response(BreathingExercise exercise) {
        return new BreathingExerciseResponse(exercise.getId(), exercise.getTitle(), exercise.getInhaleSeconds(),
                exercise.getHoldSeconds(), exercise.getExhaleSeconds(), exercise.getCycles());
    }
}
