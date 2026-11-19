package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.dto.admin.MetricsResponse;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.repository.audit.AuditEventRepository;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminMetricsService {

    public static final Duration DEFAULT_PERIOD = Duration.ofDays(30);

    private final UserRepository userRepository;
    private final DoctorPatientLinkRepository doctorLinkRepository;
    private final CaregiverPatientLinkRepository caregiverLinkRepository;
    private final AuditEventRepository auditEventRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public MetricsResponse metrics(Instant from, Instant to) {
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(DEFAULT_PERIOD) : from;
        Map<Role, MetricsResponse.RoleCount> users = new EnumMap<>(Role.class);
        for (Role role : Role.values()) {
            users.put(role, new MetricsResponse.RoleCount(userRepository.countByRoleAndActive(role, true),
                    userRepository.countByRoleAndActive(role, false)));
        }
        Map<LinkStatus, Long> doctorLinks = new EnumMap<>(LinkStatus.class);
        Map<LinkStatus, Long> caregiverLinks = new EnumMap<>(LinkStatus.class);
        for (LinkStatus status : LinkStatus.values()) {
            doctorLinks.put(status, doctorLinkRepository.countByStatus(status));
            caregiverLinks.put(status, caregiverLinkRepository.countByStatus(status));
        }
        return new MetricsResponse(users, userRepository.countByRoleAndCrmVerifiedFalse(Role.DOCTOR),
                doctorLinks, caregiverLinks,
                auditEventRepository.countByEventTypeAndOccurredAtBetween(AuditEventType.LOGIN_SUCCEEDED, start, end),
                auditEventRepository.countByEventTypeAndOccurredAtBetween(AuditEventType.LOGIN_FAILED, start, end),
                start, end);
    }
}
