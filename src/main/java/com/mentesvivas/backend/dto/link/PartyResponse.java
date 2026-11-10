package com.mentesvivas.backend.dto.link;

import java.util.UUID;

public record PartyResponse(
        UUID id,
        String fullName,
        String photoUrl
) {
}
