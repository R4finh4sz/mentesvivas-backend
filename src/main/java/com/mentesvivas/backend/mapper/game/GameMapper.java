package com.mentesvivas.backend.mapper.game;

import com.mentesvivas.backend.dto.game.FamilyMemberResponse;
import com.mentesvivas.backend.entity.game.FamilyMember;

public final class GameMapper {

    private GameMapper() {
    }

    public static FamilyMemberResponse toResponse(FamilyMember member, boolean showAudio) {
        return new FamilyMemberResponse(member.getId(), member.getName(), member.getKinship(),
                member.getPhotoUrl(), showAudio ? member.getAudioUrl() : null);
    }
}
