package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatientCareAccessTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private CurrentUser currentUser;
    @Mock
    private CaregiverPatientAccess caregiverAccess;
    @Mock
    private DoctorPatientAccess doctorAccess;

    private PatientCareAccess access;

    @BeforeEach
    void setUp() {
        access = new PatientCareAccess(currentUser, caregiverAccess, doctorAccess);
    }

    private User user(Role role, UUID id) {
        User user = new User();
        user.setId(id);
        user.setRole(role);
        when(currentUser.get()).thenReturn(user);
        return user;
    }

    @Test
    void patientRecordsAndReadsOwnData() {
        User patient = user(Role.PATIENT, PATIENT);

        assertThat(access.requireWriter(PATIENT)).isSameAs(patient);
        assertThat(access.requireReader(PATIENT)).isSameAs(patient);
    }

    @Test
    void patientCannotAccessAnotherPatient() {
        user(Role.PATIENT, UUID.randomUUID());

        assertThatThrownBy(() -> access.requireWriter(PATIENT)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.requireReader(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void caregiverNeedsToBeTheRepresentative() {
        User caregiver = user(Role.CAREGIVER, UUID.randomUUID());
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(caregiver);

        assertThat(access.requireWriter(PATIENT)).isSameAs(caregiver);
        assertThat(access.requireReader(PATIENT)).isSameAs(caregiver);
    }

    @Test
    void caregiverWithoutLinkIsDenied() {
        user(Role.CAREGIVER, UUID.randomUUID());
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenThrow(new AccessDeniedException("sem vinculo"));

        assertThatThrownBy(() -> access.requireWriter(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void doctorReadsButDoesNotRecord() {
        User doctor = user(Role.DOCTOR, UUID.randomUUID());
        when(doctorAccess.requireDoctorOf(PATIENT)).thenReturn(doctor);

        assertThat(access.requireReader(PATIENT)).isSameAs(doctor);
        assertThatThrownBy(() -> access.requireWriter(PATIENT)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void administratorHasNoAccess() {
        user(Role.ADMINISTRATOR, UUID.randomUUID());

        assertThatThrownBy(() -> access.requireWriter(PATIENT)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> access.requireReader(PATIENT)).isInstanceOf(AccessDeniedException.class);
        verifyNoInteractions(caregiverAccess, doctorAccess);
    }
}
