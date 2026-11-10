package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.dto.admin.PendingDoctorResponse;
import com.mentesvivas.backend.entity.admin.AdminAction;
import com.mentesvivas.backend.entity.admin.AdminActionType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.admin.AdminActionRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DoctorCrmVerificationService {

    private final UserRepository userRepository;
    private final AdminActionRepository actionRepository;
    private final CurrentUser currentUser;

    @Transactional(readOnly = true)
    public List<PendingDoctorResponse> pending() {
        return userRepository.findByRoleAndCrmVerifiedFalseOrderByCreatedAtAsc(Role.DOCTOR).stream()
                .map(doctor -> new PendingDoctorResponse(doctor.getId(), doctor.getFullName(), doctor.getEmail(),
                        doctor.getCrm(), doctor.getCrmState()))
                .toList();
    }

    @Transactional
    public void verify(UUID doctorId) {
        User doctor = userRepository.findById(doctorId)
                .filter(user -> user.getRole() == Role.DOCTOR)
                .orElseThrow(() -> new ResourceNotFoundException("Médico não encontrado."));
        if (doctor.isCrmVerified()) {
            return;
        }
        doctor.setCrmVerified(true);
        actionRepository.save(new AdminAction(currentUser.get().getId(), doctor.getId(),
                AdminActionType.CRM_VERIFIED, Instant.now()));
    }
}
