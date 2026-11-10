package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
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
class DoctorPatientAccessTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private CurrentUser currentUser;
    @Mock
    private DoctorPatientLinkRepository linkRepository;

    private DoctorPatientAccess access;
    private User doctor;

    @BeforeEach
    void setUp() {
        access = new DoctorPatientAccess(currentUser, linkRepository);
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setRole(Role.DOCTOR);
        doctor.setCrmVerified(true);
        when(currentUser.get()).thenReturn(doctor);
    }

    @Test
    void verifiedDoctorWithAnActiveLinkIsAllowed() {
        when(linkRepository.existsByDoctorIdAndPatientIdAndStatus(doctor.getId(), PATIENT, LinkStatus.ACTIVE))
                .thenReturn(true);

        assertThat(access.requireDoctorOf(PATIENT)).isSameAs(doctor);
    }

    @Test
    void withoutAnActiveLinkAccessIsDenied() {
        assertThatThrownBy(() -> access.requireDoctorOf(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorWithoutVerifiedCrmIsDenied() {
        doctor.setCrmVerified(false);

        assertThatThrownBy(() -> access.requireDoctorOf(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void otherProfilesAreDenied() {
        doctor.setRole(Role.CAREGIVER);

        assertThatThrownBy(() -> access.requireDoctorOf(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }
}
