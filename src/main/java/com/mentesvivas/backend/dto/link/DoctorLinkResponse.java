package com.mentesvivas.backend.dto.link;

import com.mentesvivas.backend.entity.link.LinkStatus;

import java.time.Instant;
import java.util.UUID;

public record DoctorLinkResponse(
        UUID id,
        LinkStatus status,
        DoctorSummaryResponse doctor,
        PartyResponse patient,
        boolean doctorConfirmed,
        boolean patientAuthorized,
        Instant createdAt
) {
}
