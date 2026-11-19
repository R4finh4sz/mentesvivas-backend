package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.service.auth.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

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
class AdminUserServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private RefreshTokenService refreshTokenService;
    @Mock
    private AuditTrail auditTrail;

    private AdminUserService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new AdminUserService(userRepository, currentUser, refreshTokenService, auditTrail);
        admin = user(Role.ADMINISTRATOR);
    }

    private User user(Role role) {
        User user = new User();
        user.setId(UUID.randomUUID());
        user.setRole(role);
        user.setFullName(role.name());
        user.setEmail(role.name().toLowerCase() + "@exemplo.com");
        return user;
    }

    private AuditRecord audited() {
        ArgumentCaptor<AuditRecord> captor = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(captor.capture());
        return captor.getValue();
    }

    @Test
    void deactivatingRevokesTheSessionsAndRecordsTheJustification() {
        User patient = user(Role.PATIENT);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        var response = service.deactivate(patient.getId(), "Conta comprometida");

        assertThat(response.active()).isFalse();
        assertThat(patient.isActive()).isFalse();
        verify(refreshTokenService).revokeAll(patient);
        AuditRecord record = audited();
        assertThat(record.type()).isEqualTo(AuditEventType.USER_DEACTIVATED);
        assertThat(record.actorUserId()).isEqualTo(admin.getId());
        assertThat(record.patientUserId()).isEqualTo(patient.getId());
        assertThat(record.justification()).isEqualTo("Conta comprometida");
        assertThat(record.afterValue()).contains("INACTIVE").contains(patient.getId().toString());
    }

    @Test
    void nonPatientTargetsAreNotRecordedAsPatients() {
        User doctor = user(Role.DOCTOR);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(doctor.getId())).thenReturn(Optional.of(doctor));

        service.deactivate(doctor.getId(), "Registro irregular");

        assertThat(audited().patientUserId()).isNull();
    }

    @Test
    void theAdministratorCannotBeDeactivatedNotEvenByThemselves() {
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(admin.getId())).thenReturn(Optional.of(admin));

        assertThatThrownBy(() -> service.deactivate(admin.getId(), "x")).isInstanceOf(ConflictException.class);

        assertThat(admin.isActive()).isTrue();
        verify(refreshTokenService, never()).revokeAll(any());
        verify(auditTrail, never()).record(any());
    }

    @Test
    void anotherAdministratorRoleCannotBeDeactivatedEither() {
        User otherAdmin = user(Role.ADMINISTRATOR);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(otherAdmin.getId())).thenReturn(Optional.of(otherAdmin));

        assertThatThrownBy(() -> service.deactivate(otherAdmin.getId(), "x")).isInstanceOf(ConflictException.class);
    }

    @Test
    void deactivatingAnInactiveAccountIsAConflict() {
        User patient = user(Role.PATIENT);
        patient.setActive(false);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.deactivate(patient.getId(), "x")).isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownUserIsNotFound() {
        UUID id = UUID.randomUUID();
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.deactivate(id, "x")).isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.reactivate(id, "x")).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void reactivatingRestoresTheAccountAndRecordsTheEvent() {
        User patient = user(Role.PATIENT);
        patient.setActive(false);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        var response = service.reactivate(patient.getId(), "Verificado");

        assertThat(response.active()).isTrue();
        AuditRecord record = audited();
        assertThat(record.type()).isEqualTo(AuditEventType.USER_REACTIVATED);
        assertThat(record.justification()).isEqualTo("Verificado");
    }

    @Test
    void reactivatingAnActiveAccountIsAConflict() {
        User patient = user(Role.PATIENT);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findById(patient.getId())).thenReturn(Optional.of(patient));

        assertThatThrownBy(() -> service.reactivate(patient.getId(), "x")).isInstanceOf(ConflictException.class);
    }

    @Test
    void listFiltersByRoleClampsThePageAndRecordsTheQuery() {
        User doctor = user(Role.DOCTOR);
        doctor.setCrmVerified(true);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findByRole(any(Role.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(doctor), PageRequest.of(0, 100), 1));

        var page = service.list(Role.DOCTOR, -1, 5000);

        assertThat(page.size()).isEqualTo(AdminUserService.MAX_PAGE_SIZE);
        assertThat(page.items()).singleElement().satisfies(item -> {
            assertThat(item.role()).isEqualTo(Role.DOCTOR);
            assertThat(item.crmVerified()).isTrue();
        });
        AuditRecord record = audited();
        assertThat(record.type()).isEqualTo(AuditEventType.ADMIN_USERS_LISTED);
        assertThat(record.afterValue()).isEqualTo("DOCTOR");
    }

    @Test
    void listWithoutRoleReturnsEveryone() {
        User patient = user(Role.PATIENT);
        when(currentUser.get()).thenReturn(admin);
        when(userRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(patient)));

        var page = service.list(null, 0, 20);

        assertThat(page.items()).singleElement().satisfies(item -> assertThat(item.crmVerified()).isNull());
        assertThat(audited().afterValue()).isEqualTo("ALL");
    }
}
