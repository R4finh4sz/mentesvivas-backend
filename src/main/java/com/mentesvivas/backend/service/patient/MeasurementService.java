package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.RecordMeasurementRequest;
import com.mentesvivas.backend.entity.patient.BodyMeasurement;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.BodyMeasurementRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MeasurementService {

    private final CurrentPatient currentPatient;
    private final BodyMeasurementRepository repository;
    private final MeasurementValidator validator;

    @Transactional
    public MeasurementResponse record(RecordMeasurementRequest request) {
        User patient = currentPatient.get();
        BigDecimal weight = null;
        BigDecimal height = null;

        if (request.weightKg() != null) {
            weight = request.weightKg().setScale(1, RoundingMode.HALF_UP);
            validator.validateWeight(weight, request.confirmed());
        }
        if (request.heightM() != null) {
            height = request.heightM().setScale(2, RoundingMode.HALF_UP);
            validator.validateHeight(height, request.confirmed());
        }

        BodyMeasurement measurement = new BodyMeasurement();
        measurement.setPatientId(patient.getId());
        measurement.setWeightKg(weight);
        measurement.setHeightM(height);
        measurement.setRecordedBy(patient.getId());
        measurement.setMeasuredAt(Instant.now());
        return PatientMapper.toResponse(repository.save(measurement));
    }

    @Transactional(readOnly = true)
    public List<MeasurementResponse> history() {
        return repository.findByPatientIdOrderByMeasuredAtDesc(currentPatient.get().getId()).stream()
                .map(PatientMapper::toResponse)
                .toList();
    }
}
