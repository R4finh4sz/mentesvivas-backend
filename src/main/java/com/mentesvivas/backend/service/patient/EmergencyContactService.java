package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.EmergencyContactRequest;
import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.dto.patient.ReplaceEmergencyContactsRequest;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.EmergencyContact;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.mapper.patient.PatientMapper;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EmergencyContactService {

    private final CurrentPatient currentPatient;
    private final EmergencyContactRepository repository;
    private final SensitiveChangeRecorder changeRecorder;

    @Transactional(readOnly = true)
    public List<EmergencyContactResponse> list() {
        return repository.findByPatientIdOrderByContactOrderAsc(currentPatient.get().getId()).stream()
                .map(PatientMapper::toResponse)
                .toList();
    }

    @Transactional
    public List<EmergencyContactResponse> replace(ReplaceEmergencyContactsRequest request) {
        User patient = currentPatient.get();
        repository.deleteAllByPatientId(patient.getId());
        List<EmergencyContact> saved = repository.saveAll(request.contacts().stream()
                .map(contact -> toEntity(patient.getId(), contact))
                .toList());
        changeRecorder.record(patient.getId(), SensitiveField.EMERGENCY_CONTACTS, ChangeType.REPLACED,
                "Contatos de emergencia substituidos (%d contatos)".formatted(saved.size()), patient.getId());
        return saved.stream()
                .sorted((a, b) -> a.getContactOrder().compareTo(b.getContactOrder()))
                .map(PatientMapper::toResponse)
                .toList();
    }

    private EmergencyContact toEntity(UUID patientId, EmergencyContactRequest request) {
        EmergencyContact contact = new EmergencyContact();
        contact.setPatientId(patientId);
        contact.setName(request.name());
        contact.setRelationship(request.relationship());
        contact.setPhone(request.phone());
        contact.setContactOrder(request.order());
        return contact;
    }
}
