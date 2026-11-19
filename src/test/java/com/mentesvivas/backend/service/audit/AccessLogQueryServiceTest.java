package com.mentesvivas.backend.service.audit;

import com.mentesvivas.backend.entity.audit.AccessEvent;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.audit.AccessEventRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccessLogQueryServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Mock
    private AccessEventRepository repository;
    @Mock
    private UserRepository userRepository;

    private AccessLogQueryService service;

    @BeforeEach
    void setUp() {
        service = new AccessLogQueryService(repository, userRepository);
    }

    private User doctor() {
        User doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setFullName("Dr. Paulo");
        doctor.setRole(Role.DOCTOR);
        doctor.setCrm("123456");
        doctor.setCrmState("SP");
        return doctor;
    }

    @Test
    void resolvesTheAccessorNameRoleAndCrmAtQueryTime() {
        User doctor = doctor();
        AccessEvent event = new AccessEvent(NOW, doctor.getId(), PATIENT, PatientDataArea.ALLERGIES);
        when(repository.findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(eq(PATIENT), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));
        when(userRepository.findAllById(List.of(doctor.getId()))).thenReturn(List.of(doctor));

        var page = service.forPatient(PATIENT, null, null, 0, 20);

        assertThat(page.totalItems()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(entry -> {
            assertThat(entry.accessorName()).isEqualTo("Dr. Paulo");
            assertThat(entry.accessorRole()).isEqualTo(Role.DOCTOR);
            assertThat(entry.crm()).isEqualTo("123456");
            assertThat(entry.area()).isEqualTo(PatientDataArea.ALLERGIES);
            assertThat(entry.occurredAt()).isEqualTo(NOW);
        });
    }

    @Test
    void accessorThatNoLongerExistsKeepsTheIdOnly() {
        UUID gone = UUID.randomUUID();
        AccessEvent event = new AccessEvent(NOW, gone, PATIENT, PatientDataArea.PROFILE);
        when(repository.findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(eq(PATIENT), any(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(event), PageRequest.of(0, 20), 1));
        when(userRepository.findAllById(List.of(gone))).thenReturn(List.of());

        var page = service.forPatient(PATIENT, null, null, 0, 20);

        assertThat(page.items()).singleElement().satisfies(entry -> {
            assertThat(entry.accessorId()).isEqualTo(gone);
            assertThat(entry.accessorName()).isNull();
        });
    }

    @Test
    void pageSizeIsClampedAndNegativePageBecomesZero() {
        when(repository.findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(eq(PATIENT), any(), any(), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(userRepository.findAllById(List.of())).thenReturn(List.of());

        service.forPatient(PATIENT, null, null, -3, 5000);

        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(repository).findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(eq(PATIENT), any(), any(), captor.capture());
        assertThat(captor.getValue().getPageSize()).isEqualTo(AccessLogQueryService.MAX_PAGE_SIZE);
        assertThat(captor.getValue().getPageNumber()).isZero();
    }

    @Test
    void periodFiltersAreForwardedWhenInformed() {
        Instant from = NOW.minusSeconds(3600);
        when(repository.findByPatientUserIdAndOccurredAtBetweenOrderByOccurredAtDesc(eq(PATIENT), eq(from), eq(NOW), any(Pageable.class)))
                .thenReturn(Page.empty());
        when(userRepository.findAllById(List.of())).thenReturn(List.of());

        assertThat(service.forPatient(PATIENT, from, NOW, 0, 20).items()).isEmpty();
    }
}
