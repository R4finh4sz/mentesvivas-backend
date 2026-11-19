package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AdminMetricsServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-19T12:00:00Z");

    @Mock
    private UserRepository userRepository;
    @Mock
    private DoctorPatientLinkRepository doctorLinkRepository;
    @Mock
    private CaregiverPatientLinkRepository caregiverLinkRepository;
    @Mock
    private AuditEventRepository auditEventRepository;

    private AdminMetricsService service;

    @BeforeEach
    void setUp() {
        service = new AdminMetricsService(userRepository, doctorLinkRepository, caregiverLinkRepository,
                auditEventRepository, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void aggregatesUsersLinksAndLoginsWithoutAnyIdentification() {
        when(userRepository.countByRoleAndActive(Role.PATIENT, true)).thenReturn(10L);
        when(userRepository.countByRoleAndActive(Role.PATIENT, false)).thenReturn(2L);
        when(userRepository.countByRoleAndCrmVerifiedFalse(Role.DOCTOR)).thenReturn(3L);
        when(doctorLinkRepository.countByStatus(LinkStatus.ACTIVE)).thenReturn(4L);
        when(caregiverLinkRepository.countByStatus(LinkStatus.PENDING)).thenReturn(5L);
        Instant start = NOW.minus(30, ChronoUnit.DAYS);
        when(auditEventRepository.countByEventTypeAndOccurredAtBetween(AuditEventType.LOGIN_SUCCEEDED, start, NOW)).thenReturn(100L);
        when(auditEventRepository.countByEventTypeAndOccurredAtBetween(AuditEventType.LOGIN_FAILED, start, NOW)).thenReturn(7L);

        var metrics = service.metrics(null, null);

        assertThat(metrics.usersByRole().get(Role.PATIENT).active()).isEqualTo(10);
        assertThat(metrics.usersByRole().get(Role.PATIENT).inactive()).isEqualTo(2);
        assertThat(metrics.usersByRole()).containsKeys(Role.values());
        assertThat(metrics.doctorsPendingCrm()).isEqualTo(3);
        assertThat(metrics.doctorLinks().get(LinkStatus.ACTIVE)).isEqualTo(4);
        assertThat(metrics.caregiverLinks().get(LinkStatus.PENDING)).isEqualTo(5);
        assertThat(metrics.loginsSucceeded()).isEqualTo(100);
        assertThat(metrics.loginsFailed()).isEqualTo(7);
        assertThat(metrics.from()).isEqualTo(start);
        assertThat(metrics.to()).isEqualTo(NOW);
    }

    @Test
    void informedPeriodIsUsedAsIs() {
        Instant from = NOW.minus(1, ChronoUnit.DAYS);
        Instant to = NOW.minus(1, ChronoUnit.HOURS);
        when(auditEventRepository.countByEventTypeAndOccurredAtBetween(AuditEventType.LOGIN_SUCCEEDED, from, to)).thenReturn(1L);

        var metrics = service.metrics(from, to);

        assertThat(metrics.loginsSucceeded()).isEqualTo(1);
        assertThat(metrics.from()).isEqualTo(from);
        assertThat(metrics.to()).isEqualTo(to);
    }
}
