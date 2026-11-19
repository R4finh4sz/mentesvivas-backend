package com.mentesvivas.backend.dto.audit;

import java.time.Instant;
import java.util.UUID;

public record ErrataResponse(
        UUID id,
        UUID correctsEventId,
        Instant occurredAt
) {
}
