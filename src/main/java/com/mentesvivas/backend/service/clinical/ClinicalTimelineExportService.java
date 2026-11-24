package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.auth.TwoFactorChallengeResponse;
import com.mentesvivas.backend.dto.clinical.ExportTimelineRequest;
import com.mentesvivas.backend.dto.clinical.TimelineResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.auth.Reauthentication;
import com.mentesvivas.backend.service.clinical.timeline.TimelinePdfRenderer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClinicalTimelineExportService {

    private final DoctorPatientAccess access;
    private final Reauthentication reauthentication;
    private final ClinicalTimelineService timelines;
    private final TimelinePdfRenderer renderer;
    private final UserRepository users;
    private final AuditTrail audit;

    public TwoFactorChallengeResponse challenge(UUID patientId) {
        return reauthentication.challenge(access.requireDoctorOf(patientId));
    }

    @Transactional
    public byte[] export(UUID patientId, ExportTimelineRequest request) {
        User doctor = access.requireDoctorOf(patientId);
        reauthentication.verify(doctor, request.password(), request.code());
        TimelineResponse timeline = timelines.timeline(patientId, request.from(), request.to());
        User patient = users.findById(patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente não encontrado."));
        byte[] pdf = renderer.render(patient.getFullName(), doctor.getFullName(), timeline);
        audit.record(AuditRecord.of(AuditEventType.CLINICAL_TIMELINE_EXPORTED, doctor.getId()).forPatient(patientId)
                .withValues(null, timeline.from() + "/" + timeline.to()));
        return pdf;
    }
}
