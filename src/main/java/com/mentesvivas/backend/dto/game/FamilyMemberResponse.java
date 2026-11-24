package com.mentesvivas.backend.dto.game;

import java.util.UUID;

public record FamilyMemberResponse(UUID id, String name, String kinship, String photoUrl, String audioUrl) {
}
