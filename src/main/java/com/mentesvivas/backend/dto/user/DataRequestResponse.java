package com.mentesvivas.backend.dto.user;

import java.time.Instant;
import java.util.UUID;

public record DataRequestResponse(UUID id, UUID requestedBy, Type type, Status status,
                                  Instant openedAt, Instant updatedAt) {

    public enum Type { ACCESS, CORRECTION, DELETION }

    public enum Status { OPEN, IN_REVIEW, RESOLVED, DECLINED }
}
