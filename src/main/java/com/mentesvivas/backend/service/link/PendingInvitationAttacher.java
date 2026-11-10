package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.link.PendingInvitation;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.PendingInvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class PendingInvitationAttacher {

    private final PendingInvitationRepository pendingRepository;
    private final CaregiverLinkService caregiverLinks;
    private final DoctorLinkService doctorLinks;

    public void attach(User user) {
        if (user.getRole() != Role.PATIENT) {
            return;
        }
        List<PendingInvitation> invitations = pendingRepository.findByEmail(user.getEmail());
        for (PendingInvitation invitation : invitations) {
            switch (invitation.getKind()) {
                case CAREGIVER_TO_PATIENT -> caregiverLinks.openFromPendingInvitation(invitation.getInviterId(), user);
                case DOCTOR_TO_PATIENT -> doctorLinks.openFromPendingInvitation(invitation.getInviterId(), user);
            }
        }
        pendingRepository.deleteAll(invitations);
    }
}
