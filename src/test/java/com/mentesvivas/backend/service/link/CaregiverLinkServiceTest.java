package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.InvitationKind;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.link.PendingInvitation;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.PendingInvitationRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.security.RoleRequirement;
import com.mentesvivas.backend.service.notification.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaregiverLinkServiceTest {

    @Mock
    private CurrentUser currentUser;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CaregiverPatientLinkRepository linkRepository;
    @Mock
    private PendingInvitationRepository pendingRepository;
    @Mock
    private NotificationService notifications;
    @Mock
    private LinkEmails emails;

    private CaregiverLinkService service;
    private User caregiver;
    private User patient;

    @BeforeEach
    void setUp() {
        service = new CaregiverLinkService(currentUser, new RoleRequirement(currentUser), userRepository,
                linkRepository, pendingRepository, notifications, emails);
        caregiver = user(Role.CAREGIVER, "cuidador@exemplo.com");
        patient = user(Role.PATIENT, "paciente@exemplo.com");
        lenient().when(userRepository.findById(caregiver.getId())).thenReturn(Optional.of(caregiver));
        lenient().when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));
        lenient().when(linkRepository.save(any(CaregiverPatientLink.class))).thenAnswer(call -> {
            CaregiverPatientLink link = call.getArgument(0);
            if (link.getId() == null) {
                link.setId(UUID.randomUUID());
            }
            return link;
        });
    }

    private User user(Role role, String email) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setEmail(email);
        user.setFullName(role.name());
        return user;
    }

    private CaregiverPatientLink link(LinkStatus status) {
        CaregiverPatientLink link = new CaregiverPatientLink(caregiver.getId(), patient.getId(), status);
        link.setId(UUID.randomUUID());
        return link;
    }

    @Test
    void invitingARegisteredPatientCreatesAPendingLinkNotifiesAndEmails() {
        when(currentUser.get()).thenReturn(caregiver);
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(linkRepository.findByCaregiverIdAndPatientId(caregiver.getId(), patient.getId())).thenReturn(Optional.empty());

        service.invite(patient.getEmail());

        verify(notifications).notify(eq(patient.getId()), eq(NotificationType.CAREGIVER_INVITATION), any(), anyString());
        verify(emails).inviteRegistered(patient.getEmail());
    }

    @Test
    void invitingAnUnknownEmailKeepsAPendingInvitationOnce() {
        when(currentUser.get()).thenReturn(caregiver);
        when(userRepository.findByEmail("novo@exemplo.com")).thenReturn(Optional.empty());
        when(pendingRepository.existsByEmailAndInviterIdAndKind("novo@exemplo.com", caregiver.getId(),
                InvitationKind.CAREGIVER_TO_PATIENT)).thenReturn(false, true);

        service.invite("novo@exemplo.com");
        service.invite("novo@exemplo.com");

        verify(pendingRepository).save(any(PendingInvitation.class));
        verify(emails, org.mockito.Mockito.times(2)).inviteToRegister("novo@exemplo.com");
    }

    @Test
    void invitingSomeoneWhoIsNotAPatientDoesNothing() {
        when(currentUser.get()).thenReturn(caregiver);
        User other = user(Role.DOCTOR, "dr@exemplo.com");
        when(userRepository.findByEmail("dr@exemplo.com")).thenReturn(Optional.of(other));

        service.invite("dr@exemplo.com");

        verify(linkRepository, never()).save(any());
        verify(notifications, never()).notify(any(), any(), any(), anyString());
    }

    @Test
    void anOpenLinkIsNotInvitedAgain() {
        when(currentUser.get()).thenReturn(caregiver);
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(linkRepository.findByCaregiverIdAndPatientId(caregiver.getId(), patient.getId()))
                .thenReturn(Optional.of(link(LinkStatus.ACTIVE)));

        service.invite(patient.getEmail());

        verify(linkRepository, never()).save(any());
        verify(emails, never()).inviteRegistered(anyString());
    }

    @Test
    void onlyCaregiversCanInvite() {
        when(currentUser.get()).thenReturn(patient);

        assertThatThrownBy(() -> service.invite("x@exemplo.com")).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void thePatientAcceptsAndTheCaregiverIsNotified() {
        CaregiverPatientLink pending = link(LinkStatus.PENDING);
        when(currentUser.get()).thenReturn(patient);
        when(linkRepository.findById(pending.getId())).thenReturn(Optional.of(pending));

        var response = service.accept(pending.getId());

        assertThat(response.status()).isEqualTo(LinkStatus.ACTIVE);
        verify(notifications).notify(eq(caregiver.getId()), eq(NotificationType.LINK_ACCEPTED), eq(pending.getId()), anyString());
    }

    @Test
    void anotherPatientCannotAnswerAndAnsweredInvitationsAreConflicts() {
        CaregiverPatientLink pending = link(LinkStatus.PENDING);
        when(linkRepository.findById(pending.getId())).thenReturn(Optional.of(pending));
        when(currentUser.get()).thenReturn(user(Role.PATIENT, "outro@exemplo.com"));
        assertThatThrownBy(() -> service.accept(pending.getId())).isInstanceOf(AccessDeniedException.class);

        pending.setStatus(LinkStatus.DECLINED);
        when(currentUser.get()).thenReturn(patient);
        assertThatThrownBy(() -> service.decline(pending.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void endingNotifiesTheOtherPartyAndClosedLinksCannotBeEndedAgain() {
        CaregiverPatientLink active = link(LinkStatus.ACTIVE);
        when(currentUser.get()).thenReturn(caregiver);
        when(linkRepository.findById(active.getId())).thenReturn(Optional.of(active));

        service.end(active.getId());

        assertThat(active.getStatus()).isEqualTo(LinkStatus.ENDED);
        verify(notifications).notify(eq(patient.getId()), eq(NotificationType.LINK_ENDED), eq(active.getId()), anyString());
        assertThatThrownBy(() -> service.end(active.getId())).isInstanceOf(ConflictException.class);
    }
}
