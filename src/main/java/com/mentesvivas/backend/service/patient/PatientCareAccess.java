package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PatientCareAccess {

    private final CurrentUser currentUser;
    private final CaregiverPatientAccess caregiverAccess;
    private final DoctorPatientAccess doctorAccess;
    private final PatientAutonomyService autonomy;

    public User requireWriter(UUID patientId) {
        User user = currentUser.get();
        if (user.getRole() == Role.PATIENT) {
            return requireSelf(user, patientId);
        }
        if (user.getRole() == Role.CAREGIVER) {
            return caregiverAccess.requireRepresentativeOf(patientId);
        }
        throw new AccessDeniedException("Seu perfil nao registra dados de cuidado.");
    }

    public User requireReader(UUID patientId) {
        if (currentUser.get().getRole() == Role.DOCTOR) {
            return doctorAccess.requireDoctorOf(patientId);
        }
        return requireWriter(patientId);
    }

    public void requireCareRecordsAllowed(User actor) {
        if (actor.getRole() == Role.PATIENT && autonomy.settings(actor.getId()).restrictCareRecords()) {
            throw new AccessDeniedException("O registro de cuidado está restrito no modo assistido.");
        }
    }

    public void requireGamesAllowed(User actor) {
        if (actor.getRole() == Role.PATIENT && autonomy.settings(actor.getId()).restrictGames()) {
            throw new AccessDeniedException("Os jogos estão restritos no modo assistido.");
        }
    }

    private User requireSelf(User user, UUID patientId) {
        if (!user.getId().equals(patientId)) {
            throw new AccessDeniedException("Voce so acessa os seus proprios dados de cuidado.");
        }
        return user;
    }
}
