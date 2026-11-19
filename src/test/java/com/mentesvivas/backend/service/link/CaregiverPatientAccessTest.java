package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaregiverPatientAccessTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private CurrentUser currentUser;
    @Mock
    private CaregiverPatientLinkRepository linkRepository;

    private CaregiverPatientAccess access;
    private User caregiver;

    @BeforeEach
    void setUp() {
        access = new CaregiverPatientAccess(currentUser, linkRepository);
        caregiver = new User();
        caregiver.setId(UUID.randomUUID());
        caregiver.setRole(Role.CAREGIVER);
    }

    @Test
    void activeLinkMakesTheCaregiverTheRepresentative() {
        when(linkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiver.getId(), PATIENT, LinkStatus.ACTIVE))
                .thenReturn(true);

        assertThat(access.isRepresentative(caregiver.getId(), PATIENT)).isTrue();
    }

    @Test
    void anyOtherLinkStatusDoesNotRepresent() {
        when(linkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiver.getId(), PATIENT, LinkStatus.ACTIVE))
                .thenReturn(false);

        assertThat(access.isRepresentative(caregiver.getId(), PATIENT)).isFalse();
    }

    @Test
    void requireReturnsTheCurrentCaregiverWhenRepresentative() {
        when(currentUser.get()).thenReturn(caregiver);
        when(linkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiver.getId(), PATIENT, LinkStatus.ACTIVE))
                .thenReturn(true);

        assertThat(access.requireRepresentativeOf(PATIENT)).isSameAs(caregiver);
    }

    @Test
    void requireDeniesCaregiversWithoutAnActiveLink() {
        when(currentUser.get()).thenReturn(caregiver);
        when(linkRepository.existsByCaregiverIdAndPatientIdAndStatus(caregiver.getId(), PATIENT, LinkStatus.ACTIVE))
                .thenReturn(false);

        assertThatThrownBy(() -> access.requireRepresentativeOf(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void requireDeniesOtherProfiles() {
        caregiver.setRole(Role.DOCTOR);
        when(currentUser.get()).thenReturn(caregiver);

        assertThatThrownBy(() -> access.requireRepresentativeOf(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }
}
