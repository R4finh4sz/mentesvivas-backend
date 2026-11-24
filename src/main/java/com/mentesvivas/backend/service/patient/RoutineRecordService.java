package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.dto.patient.RoutineRecordResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.RoutineRecord;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.RoutineRecordRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.patient.routine.RoutineValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RoutineRecordService {

    private final PatientCareAccess access;
    private final RoutineRecordRepository repository;
    private final RoutineValidator validator;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public RoutineRecordResponse record(UUID patientId, RecordRoutineRequest request) {
        User author = access.requireWriter(patientId);
        access.requireCareRecordsAllowed(author);
        validator.validate(request);

        Instant now = clock.instant();
        RoutineRecord record = new RoutineRecord();
        record.setPatientId(patientId);
        record.setType(request.type());
        record.setMood(request.mood());
        record.setSleepQuality(request.sleepQuality());
        record.setSleepHours(scaled(request.sleepHours()));
        record.setMealPeriod(request.mealPeriod());
        record.setMealAccepted(request.mealAccepted());
        record.setAmountMl(request.amountMl());
        record.setBathCooperation(request.bathCooperation());
        record.setUrineFrequency(request.urineFrequency());
        record.setUrineAspect(request.urineAspect());
        record.setStoolFrequency(request.stoolFrequency());
        record.setBristolScale(request.bristolScale());
        record.setNotes(request.notes());
        record.setSource(request.source() == null ? MeasurementSource.TYPED : request.source());
        record.setRecordedBy(author.getId());
        record.setOccurredAt(request.occurredAt() == null ? now : request.occurredAt());
        record.setRecordedAt(now);
        return PatientMapper.toResponse(repository.save(record));
    }

    @Transactional
    public List<RoutineRecordResponse> history(UUID patientId, RoutineType type) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.ROUTINE);
        List<RoutineRecord> records = type == null
                ? repository.findByPatientIdOrderByOccurredAtDesc(patientId)
                : repository.findByPatientIdAndTypeOrderByOccurredAtDesc(patientId, type);
        return records.stream().map(PatientMapper::toResponse).toList();
    }

    private BigDecimal scaled(BigDecimal hours) {
        return hours == null ? null : hours.setScale(1, RoundingMode.HALF_UP);
    }
}
