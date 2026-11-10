package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.admin.AdminAction;
import com.mentesvivas.backend.entity.admin.AdminActionType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.admin.AdminActionRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorCrmVerificationServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private AdminActionRepository actionRepository;
    @Mock
    private CurrentUser currentUser;

    private DoctorCrmVerificationService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new DoctorCrmVerificationService(userRepository, actionRepository, currentUser);
        admin = user(Role.ADMINISTRATOR);
    }

    private User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        return user;
    }

    @Test
    void verifyMarksTheCrmAsVerifiedAndRecordsTheAdmin() {
        User doctor = user(Role.DOCTOR);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));
        when(currentUser.get()).thenReturn(admin);

        service.verify(doctor.getId());

        assertThat(doctor.isCrmVerified()).isTrue();
        ArgumentCaptor<AdminAction> captor = ArgumentCaptor.forClass(AdminAction.class);
        verify(actionRepository).save(captor.capture());
        assertThat(captor.getValue().getAdminId()).isEqualTo(admin.getId());
        assertThat(captor.getValue().getTargetUserId()).isEqualTo(doctor.getId());
        assertThat(captor.getValue().getAction()).isEqualTo(AdminActionType.CRM_VERIFIED);
    }

    @Test
    void verifyingAnAlreadyVerifiedDoctorDoesNotRecordAgain() {
        User doctor = user(Role.DOCTOR);
        doctor.setCrmVerified(true);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        service.verify(doctor.getId());

        verify(actionRepository, never()).save(any());
    }

    @Test
    void onlyDoctorsCanBeVerified() {
        User patient = user(Role.PATIENT);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.verify(patient.getId())).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.verify(UUID.randomUUID())).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pendingListsUnverifiedDoctors() {
        User doctor = user(Role.DOCTOR);
        doctor.setFullName("Dr. Paulo");
        doctor.setCrm("123456");
        doctor.setCrmState("SP");
        when(userRepository.findByRoleAndCrmVerifiedFalseOrderByCreatedAtAsc(Role.DOCTOR)).thenReturn(List.of(doctor));

        var result = service.pending();

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(doctor.getId());
            assertThat(item.crm()).isEqualTo("123456");
            assertThat(item.crmState()).isEqualTo("SP");
        });
    }
}
