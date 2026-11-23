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
public class VitalSignAccess {

    private final CurrentUser currentUser;
    private final CaregiverPatientAccess caregiverAccess;
    private final DoctorPatientAccess doctorAccess;

    public User requireWriter(UUID patientId) {
        User user = currentUser.get();
        if (user.getRole() == Role.PATIENT) {
            return requireSelf(user, patientId);
        }
        if (user.getRole() == Role.CAREGIVER) {
            return caregiverAccess.requireRepresentativeOf(patientId);
        }
        throw new AccessDeniedException("Seu perfil nao registra afericoes.");
    }

    public User requireReader(UUID patientId) {
        if (currentUser.get().getRole() == Role.DOCTOR) {
            return doctorAccess.requireDoctorOf(patientId);
        }
        return requireWriter(patientId);
    }

    private User requireSelf(User user, UUID patientId) {
        if (!user.getId().equals(patientId)) {
            throw new AccessDeniedException("Voce so acessa as suas proprias afericoes.");
        }
        return user;
    }
}
