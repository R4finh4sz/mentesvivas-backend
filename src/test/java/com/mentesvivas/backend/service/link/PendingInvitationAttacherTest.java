package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.link.InvitationKind;
import com.mentesvivas.backend.entity.link.PendingInvitation;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.PendingInvitationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PendingInvitationAttacherTest {

    @Mock
    private PendingInvitationRepository pendingRepository;
    @Mock
    private CaregiverLinkService caregiverLinks;
    @Mock
    private DoctorLinkService doctorLinks;

    private PendingInvitationAttacher attacher;
    private User patient;

    @BeforeEach
    void setUp() {
        attacher = new PendingInvitationAttacher(pendingRepository, caregiverLinks, doctorLinks);
        patient = new User();
        patient.setId(UUID.randomUUID());
        patient.setRole(Role.PATIENT);
        patient.setEmail("novo@exemplo.com");
    }

    @Test
    void eachPendingInvitationBecomesALinkAndIsRemoved() {
        UUID caregiverId = UUID.randomUUID();
        UUID doctorId = UUID.randomUUID();
        List<PendingInvitation> invitations = List.of(
                new PendingInvitation("novo@exemplo.com", caregiverId, InvitationKind.CAREGIVER_TO_PATIENT),
                new PendingInvitation("novo@exemplo.com", doctorId, InvitationKind.DOCTOR_TO_PATIENT));
        when(pendingRepository.findByEmail("novo@exemplo.com")).thenReturn(invitations);

        attacher.attach(patient);

        verify(caregiverLinks).openFromPendingInvitation(caregiverId, patient);
        verify(doctorLinks).openFromPendingInvitation(doctorId, patient);
        verify(pendingRepository).deleteAll(invitations);
    }

    @Test
    void otherProfilesKeepNoInvitations() {
        patient.setRole(Role.CAREGIVER);

        attacher.attach(patient);

        verifyNoInteractions(pendingRepository, caregiverLinks, doctorLinks);
    }

    @Test
    void nothingIsDoneWithoutInvitations() {
        when(pendingRepository.findByEmail("novo@exemplo.com")).thenReturn(List.of());

        attacher.attach(patient);

        org.mockito.Mockito.verify(caregiverLinks, org.mockito.Mockito.never()).openFromPendingInvitation(any(), any());
    }
}
