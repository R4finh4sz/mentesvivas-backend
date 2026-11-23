package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.RecordVitalSignRequest;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.patient.MeasurementSource;
import com.mentesvivas.backend.entity.patient.VitalSign;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.VitalSignRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VitalSignService {

    private final VitalSignAccess access;
    private final VitalSignRepository repository;
    private final VitalSignValidator validator;
    private final AccessLog accessLog;
    private final Clock clock;

    @Transactional
    public VitalSignResponse record(UUID patientId, RecordVitalSignRequest request) {
        User author = access.requireWriter(patientId);
        validator.validate(request);

        VitalSign sign = new VitalSign();
        sign.setPatientId(patientId);
        sign.setSystolic(request.systolic());
        sign.setDiastolic(request.diastolic());
        sign.setHeartRate(request.heartRate());
        sign.setGlucose(request.glucose());
        sign.setGlucoseType(request.glucoseType());
        sign.setTemperature(scaled(request.temperature()));
        sign.setOxygenSaturation(request.oxygenSaturation());
        sign.setNotes(request.notes());
        sign.setSource(request.source() == null ? MeasurementSource.TYPED : request.source());
        sign.setRecordedBy(author.getId());
        sign.setMeasuredAt(clock.instant());
        return PatientMapper.toResponse(repository.save(sign));
    }

    @Transactional
    public List<VitalSignResponse> history(UUID patientId) {
        User reader = access.requireReader(patientId);
        accessLog.record(reader.getId(), patientId, PatientDataArea.MEASUREMENTS);
        return repository.findByPatientIdOrderByMeasuredAtDesc(patientId).stream()
                .map(PatientMapper::toResponse)
                .toList();
    }

    private BigDecimal scaled(BigDecimal temperature) {
        return temperature == null ? null : temperature.setScale(1, RoundingMode.HALF_UP);
    }
}
