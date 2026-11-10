package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.dto.link.RequestDoctorLinkRequest;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.notification.NotificationType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
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

import java.time.Instant;
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
class DoctorLinkServiceTest {

    @Mock
    private CurrentUser currentUser;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorPatientLinkRepository linkRepository;
    @Mock
    private CaregiverPatientLinkRepository caregiverLinkRepository;
    @Mock
    private PendingInvitationRepository pendingRepository;
    @Mock
    private NotificationService notifications;
    @Mock
    private LinkEmails emails;

    private DoctorLinkService service;
    private User doctor;
    private User patient;
    private User caregiver;

    @BeforeEach
    void setUp() {
        service = new DoctorLinkService(currentUser, new RoleRequirement(currentUser), userRepository, linkRepository,
                caregiverLinkRepository, pendingRepository, notifications, emails);
        doctor = user(Role.DOCTOR, "dr@clinica.com");
        doctor.setCrmVerified(true);
        patient = user(Role.PATIENT, "paciente@exemplo.com");
        caregiver = user(Role.CAREGIVER, "cuidador@exemplo.com");
        for (User user : new User[]{doctor, patient, caregiver}) {
            lenient().when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        }
        lenient().when(linkRepository.save(any(DoctorPatientLink.class))).thenAnswer(call -> {
            DoctorPatientLink link = call.getArgument(0);
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

    private DoctorPatientLink pending(boolean doctorConfirmed, boolean patientAuthorized, User initiator) {
        DoctorPatientLink link = new DoctorPatientLink(doctor.getId(), patient.getId(), LinkStatus.PENDING, Instant.now());
        link.setId(UUID.randomUUID());
        link.setDoctorConfirmed(doctorConfirmed);
        link.setPatientAuthorized(patientAuthorized);
        link.setInitiatedBy(initiator.getId());
        lenient().when(linkRepository.findById(link.getId())).thenReturn(Optional.of(link));
        return link;
    }

    @Test
    void patientRequestIsAuthorizedByThePatientAndWaitsForTheDoctor() {
        when(currentUser.get()).thenReturn(patient);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(linkRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())).thenReturn(Optional.empty());

        var response = service.request(new RequestDoctorLinkRequest(doctor.getId(), null));

        assertThat(response.status()).isEqualTo(LinkStatus.PENDING);
        assertThat(response.patientAuthorized()).isTrue();
        assertThat(response.doctorConfirmed()).isFalse();
        verify(notifications).notify(eq(doctor.getId()), eq(NotificationType.DOCTOR_LINK_REQUEST), any(), anyString());
    }

    @Test
    void caregiverRequestAlsoNeedsThePatientAuthorization() {
        when(currentUser.get()).thenReturn(caregiver);
        when(caregiverLinkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiver.getId(), patient.getId(),
                LinkStatus.ACTIVE)).thenReturn(true);
        when(linkRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())).thenReturn(Optional.empty());

        var response = service.request(new RequestDoctorLinkRequest(doctor.getId(), patient.getId()));

        assertThat(response.patientAuthorized()).isFalse();
        verify(notifications).notify(eq(patient.getId()), eq(NotificationType.DOCTOR_LINK_REQUEST), any(), anyString());
    }

    @Test
    void caregiverWithoutAnActiveLinkIsDenied() {
        when(currentUser.get()).thenReturn(caregiver);

        assertThatThrownBy(() -> service.request(new RequestDoctorLinkRequest(doctor.getId(), patient.getId())))
                .isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.request(new RequestDoctorLinkRequest(doctor.getId(), null)))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorWithoutVerifiedCrmCannotBeRequested() {
        doctor.setCrmVerified(false);
        when(currentUser.get()).thenReturn(patient);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        assertThatThrownBy(() -> service.request(new RequestDoctorLinkRequest(doctor.getId(), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void openLinkBlocksANewRequest() {
        when(currentUser.get()).thenReturn(patient);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        DoctorPatientLink open = pending(false, true, patient);
        when(linkRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())).thenReturn(Optional.of(open));

        assertThatThrownBy(() -> service.request(new RequestDoctorLinkRequest(doctor.getId(), null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void linkBecomesActiveOnlyWhenBothPartiesAgree() {
        DoctorPatientLink link = pending(false, false, caregiver);

        when(currentUser.get()).thenReturn(doctor);
        assertThat(service.accept(link.getId()).status()).isEqualTo(LinkStatus.PENDING);
        verify(notifications, never()).notify(any(), eq(NotificationType.LINK_ACCEPTED), any(), anyString());

        when(currentUser.get()).thenReturn(patient);
        assertThat(service.accept(link.getId()).status()).isEqualTo(LinkStatus.ACTIVE);
        verify(notifications).notify(eq(doctor.getId()), eq(NotificationType.LINK_ACCEPTED), any(), anyString());
        verify(notifications).notify(eq(caregiver.getId()), eq(NotificationType.LINK_ACCEPTED), any(), anyString());
    }

    @Test
    void caregiversAndStrangersCannotAnswer() {
        DoctorPatientLink link = pending(false, true, patient);

        when(currentUser.get()).thenReturn(caregiver);
        assertThatThrownBy(() -> service.accept(link.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.decline(link.getId())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.end(link.getId())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void declinedAndEndedLinksCannotBeAnsweredOrEndedAgain() {
        DoctorPatientLink link = pending(true, false, doctor);
        when(currentUser.get()).thenReturn(patient);

        service.decline(link.getId());

        assertThat(link.getStatus()).isEqualTo(LinkStatus.DECLINED);
        assertThatThrownBy(() -> service.accept(link.getId())).isInstanceOf(ConflictException.class);
        assertThatThrownBy(() -> service.end(link.getId())).isInstanceOf(ConflictException.class);
    }

    @Test
    void doctorInvitationNeedsAVerifiedCrmAndSkipsNonPatients() {
        when(currentUser.get()).thenReturn(doctor);
        when(userRepository.findByEmail(caregiver.getEmail())).thenReturn(Optional.of(caregiver));

        service.invitePatient(caregiver.getEmail());
        verify(linkRepository, never()).save(any());

        doctor.setCrmVerified(false);
        assertThatThrownBy(() -> service.invitePatient(patient.getEmail())).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorInvitationToARegisteredPatientIsConfirmedByTheDoctorOnly() {
        when(currentUser.get()).thenReturn(doctor);
        when(userRepository.findByEmail(patient.getEmail())).thenReturn(Optional.of(patient));
        when(linkRepository.findByDoctorIdAndPatientId(doctor.getId(), patient.getId())).thenReturn(Optional.empty());

        service.invitePatient(patient.getEmail());

        verify(linkRepository).save(org.mockito.ArgumentMatchers.argThat(link ->
                link.isDoctorConfirmed() && !link.isPatientAuthorized() && link.getStatus() == LinkStatus.PENDING));
        verify(notifications).notify(eq(patient.getId()), eq(NotificationType.DOCTOR_INVITATION), any(), anyString());
    }
}
