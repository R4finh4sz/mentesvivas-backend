package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.AgendaProperties;
import com.mentesvivas.backend.dto.patient.AppointmentResponse;
import com.mentesvivas.backend.dto.patient.SaveAppointmentRequest;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.Appointment;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AppointmentRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AppointmentService {

    private final PatientCareAccess access;
    private final AppointmentRepository repository;
    private final AccessLog accessLog;
    private final AgendaProperties properties;
    private final Clock clock;

    @Transactional
    public AppointmentResponse create(UUID patientId, SaveAppointmentRequest request) {
        User author = access.requireWriter(patientId);
        Appointment appointment = new Appointment();
        appointment.setPatientId(patientId);
        appointment.setCreatedBy(author.getId());
        appointment.setCreatedAt(clock.instant());
        fill(appointment, request);
        return PatientMapper.toResponse(repository.save(appointment));
    }

    @Transactional
    public List<AppointmentResponse> list(UUID patientId, Instant from, Instant to) {
        User reader = access.requireWriter(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.APPOINTMENTS);
        Instant start = from == null ? clock.instant() : from;
        List<Appointment> found = to == null
                ? repository.findByPatientIdAndScheduledAtGreaterThanEqualOrderByScheduledAtAsc(patientId, start)
                : repository.findByPatientIdAndScheduledAtBetweenOrderByScheduledAtAsc(patientId, start, to);
        return found.stream().map(PatientMapper::toResponse).toList();
    }

    @Transactional
    public AppointmentResponse update(UUID patientId, UUID appointmentId, SaveAppointmentRequest request) {
        access.requireWriter(patientId);
        Appointment appointment = find(patientId, appointmentId);
        fill(appointment, request);
        return PatientMapper.toResponse(repository.save(appointment));
    }

    @Transactional
    public void delete(UUID patientId, UUID appointmentId) {
        access.requireWriter(patientId);
        repository.delete(find(patientId, appointmentId));
    }

    private Appointment find(UUID patientId, UUID appointmentId) {
        return repository.findByIdAndPatientId(appointmentId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Compromisso nao encontrado."));
    }

    private void fill(Appointment appointment, SaveAppointmentRequest request) {
        if (!request.scheduledAt().isAfter(clock.instant())) {
            throw new ImpossibleValueException("A data do compromisso deve ser futura.");
        }
        appointment.setType(request.type());
        appointment.setTitle(request.title().trim());
        appointment.setLocation(clean(request.location()));
        appointment.setNotes(clean(request.notes()));
        appointment.setScheduledAt(request.scheduledAt());
        appointment.setRemindAt(request.scheduledAt().minus(properties.reminderLead()));
    }

    private String clean(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        return text.trim();
    }
}
