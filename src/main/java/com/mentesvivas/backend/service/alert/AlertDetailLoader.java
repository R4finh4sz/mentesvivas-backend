package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.entity.alert.SosAlert;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.mapper.alert.AlertMapper;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AlertDetailLoader {

    private final AllergyRepository allergyRepository;
    private final EmergencyContactRepository contactRepository;

    public AlertDetailResponse load(SosAlert alert) {
        return new AlertDetailResponse(
                AlertMapper.toResponse(alert),
                allergyRepository.findByPatientIdOrderByCreatedAtAsc(alert.getPatientId()).stream()
                        .filter(allergy -> allergy.getSeverity() == AllergySeverity.SEVERE)
                        .map(PatientMapper::toResponse)
                        .toList(),
                contactRepository.findByPatientIdOrderByContactOrderAsc(alert.getPatientId()).stream()
                        .map(PatientMapper::toResponse)
                        .toList());
    }
}
