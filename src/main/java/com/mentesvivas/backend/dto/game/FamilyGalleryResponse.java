package com.mentesvivas.backend.dto.game;

import java.util.List;

public record FamilyGalleryResponse(
        List<FamilyMemberResponse> members,
        int minimumForRecognitionGame,
        boolean recognitionGameUnlocked,
        String guidance
) {
}
