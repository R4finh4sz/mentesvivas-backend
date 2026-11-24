package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.config.BingoProperties;
import com.mentesvivas.backend.dto.game.BingoRoundResponse;
import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSound;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.game.BingoSoundRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class BingoRoundView {

    private final BingoSoundRepository sounds;
    private final BingoSettingsService settings;
    private final BingoProperties properties;
    private final BingoFeedback feedback;

    public Map<UUID, BingoSound> soundsOf(BingoRound round) {
        Map<UUID, BingoSound> found = sounds.findAllById(round.options()).stream()
                .collect(Collectors.toMap(BingoSound::getId, Function.identity()));
        if (found.size() != round.options().size() || !found.containsKey(round.getSoundId())) {
            throw new ConflictException("A biblioteca de sons mudou. Inicie um novo jogo.");
        }
        return found;
    }

    public BingoRoundResponse of(BingoRound round, int totalRounds, UUID patientId) {
        Map<UUID, BingoSound> found = soundsOf(round);
        List<BingoRoundResponse.Option> options = round.options().stream()
                .map(id -> new BingoRoundResponse.Option(id, found.get(id).getInstrument()))
                .toList();
        boolean narration = settings.isNarrationEnabled(patientId);
        return new BingoRoundResponse(round.getId(), round.getPosition(), totalRounds,
                found.get(round.getSoundId()).getAudioUrl(), properties.soundSeconds(), narration,
                narration ? feedback.narration() : null, options);
    }
}
