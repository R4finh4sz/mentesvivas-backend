package com.mentesvivas.backend.dto.admin;

import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.Role;

import java.time.Instant;
import java.util.Map;

public record MetricsResponse(
        Map<Role, RoleCount> usersByRole,
        long doctorsPendingCrm,
        Map<LinkStatus, Long> doctorLinks,
        Map<LinkStatus, Long> caregiverLinks,
        long loginsSucceeded,
        long loginsFailed,
        Instant from,
        Instant to
) {

    public record RoleCount(long active, long inactive) {
    }
}
