package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class DoctorPatientAccess {

    private final CurrentUser currentUser;
    private final DoctorPatientLinkRepository linkRepository;

    public User requireDoctorOf(UUID patientId) {
        User doctor = currentUser.get();
        if (doctor.getRole() != Role.DOCTOR) {
            throw new AccessDeniedException("Acesso restrito a medicos.");
        }
        if (!doctor.isCrmVerified()) {
            throw new AccessDeniedException("O registro profissional ainda nao foi conferido.");
        }
        if (!linkRepository.existsByDoctorIdAndPatientIdAndStatus(doctor.getId(), patientId, LinkStatus.ACTIVE)) {
            throw new AccessDeniedException("Voce nao tem vinculo ativo com este paciente.");
        }
        return doctor;
    }
}
