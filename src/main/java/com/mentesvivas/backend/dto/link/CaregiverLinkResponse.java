package com.mentesvivas.backend.dto.link;

import com.mentesvivas.backend.entity.link.LinkStatus;

import java.time.Instant;
import java.util.UUID;

public record CaregiverLinkResponse(
        UUID id,
        LinkStatus status,
        PartyResponse caregiver,
        PartyResponse patient,
        Instant createdAt
) {
}
