package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class SosRecipients {

    private final CaregiverPatientLinkRepository linkRepository;
    private final EmergencyContactRepository contactRepository;

    public List<UUID> requireCaregiversOrContacts(UUID patientId) {
        List<UUID> caregivers = linkRepository.findByPatientIdAndStatus(patientId, LinkStatus.ACTIVE).stream()
                .map(CaregiverPatientLink::getCaregiverId)
                .toList();
        if (caregivers.isEmpty() && contactRepository.countByPatientId(patientId) == 0) {
            throw new ConflictException("O SOS precisa de um cuidador vinculado ou de um contato de emergencia. "
                    + "Cadastre um deles antes de usar o SOS.");
        }
        return caregivers;
    }
}
