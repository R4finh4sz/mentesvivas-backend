package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.AgitationProtocolResponse;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.AgitationProtocol;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AgitationProtocolRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AgitationProtocolService {

    private final AgitationProtocolRepository repository;
    private final EmergencyContactRepository contacts;
    private final DoctorPatientAccess doctors;
    private final CaregiverPatientAccess caregivers;
    private final AccessLog accessLog;
    private final AuditTrail audit;
    private final Clock clock;

    @Transactional
    public AgitationProtocolResponse publish(OccurrenceSeverity severity, String instructions) {
        UUID doctorId = doctors.requireDoctor().getId();
        AgitationProtocol protocol = repository.save(
                new AgitationProtocol(severity, instructions.trim(), doctorId, clock.instant()));
        audit.record(AuditRecord.of(AuditEventType.AGITATION_PROTOCOL_PUBLISHED, doctorId)
                .withValues(null, protocol.getId().toString()));
        return view(protocol);
    }

    @Transactional
    public CrisisDetails crisis(UUID patientId, OccurrenceSeverity severity) {
        UUID caregiverId = caregivers.requireRepresentativeOf(patientId).getId();
        accessLog.record(caregiverId, patientId, PatientDataArea.EMERGENCY_CONTACTS);
        List<EmergencyContactResponse> emergencyContacts = contacts.findByPatientIdOrderByContactOrderAsc(patientId)
                .stream().map(PatientMapper::toResponse).toList();
        return new CrisisDetails(forSeverity(severity), emergencyContacts);
    }

    public AgitationProtocolResponse forSeverity(OccurrenceSeverity severity) {
        return repository.findFirstBySeverityOrderByApprovedAtDescIdDesc(severity)
                .map(AgitationProtocolService::view).orElse(null);
    }

    private static AgitationProtocolResponse view(AgitationProtocol protocol) {
        return new AgitationProtocolResponse(protocol.getId(), protocol.getSeverity(), protocol.getInstructions(),
                protocol.getApprovedAt());
    }

    public record CrisisDetails(AgitationProtocolResponse protocol, List<EmergencyContactResponse> emergencyContacts) {}
}
