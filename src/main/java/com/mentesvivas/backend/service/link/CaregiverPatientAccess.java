package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CaregiverPatientAccess {

    private final CurrentUser currentUser;
    private final CaregiverPatientLinkRepository linkRepository;

    public boolean isRepresentative(UUID caregiverId, UUID patientId) {
        return linkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiverId, patientId, LinkStatus.ACTIVE);
    }

    public User requireRepresentativeOf(UUID patientId) {
        User caregiver = currentUser.get();
        if (caregiver.getRole() != Role.CAREGIVER) {
            throw new AccessDeniedException("Acesso restrito a cuidadores.");
        }
        if (!isRepresentative(caregiver.getId(), patientId)) {
            throw new AccessDeniedException("Voce nao e cuidador ativo deste paciente.");
        }
        return caregiver;
    }
}
