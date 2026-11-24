package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.BingoSoundResponse;
import com.mentesvivas.backend.dto.game.SaveBingoSoundRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.game.BingoSound;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.game.BingoSoundRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.util.PlainText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BingoSoundService {

    private final BingoSoundRepository repository;
    private final CurrentUser currentUser;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<BingoSoundResponse> list() {
        return repository.findAllByOrderByInstrumentAsc().stream().map(BingoSoundService::toResponse).toList();
    }

    @Transactional
    public BingoSoundResponse create(SaveBingoSoundRequest request) {
        String key = PlainText.key(request.instrument());
        if (repository.existsBySearchKey(key)) {
            throw new ConflictException("Este instrumento ja esta na biblioteca de sons.");
        }
        BingoSound sound = new BingoSound();
        sound.setCreatedAt(clock.instant());
        fill(sound, request, key);
        return save(sound);
    }

    @Transactional
    public BingoSoundResponse update(UUID id, SaveBingoSoundRequest request) {
        BingoSound sound = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Som nao encontrado na biblioteca."));
        String key = PlainText.key(request.instrument());
        if (repository.existsBySearchKeyAndIdNot(key, id)) {
            throw new ConflictException("Ja existe outro som com este instrumento na biblioteca.");
        }
        fill(sound, request, key);
        return save(sound);
    }

    private void fill(BingoSound sound, SaveBingoSoundRequest request, String key) {
        sound.setInstrument(request.instrument().trim());
        sound.setSearchKey(key);
        sound.setAudioUrl(request.audioUrl().trim());
        sound.setActive(request.isActive());
    }

    private BingoSoundResponse save(BingoSound sound) {
        BingoSound saved = repository.save(sound);
        auditTrail.record(AuditRecord.of(AuditEventType.BINGO_SOUND_CHANGED, currentUser.get().getId())
                .withValues(null, saved.getInstrument()));
        return toResponse(saved);
    }

    private static BingoSoundResponse toResponse(BingoSound sound) {
        return new BingoSoundResponse(sound.getId(), sound.getInstrument(), sound.getAudioUrl(), sound.isActive());
    }
}
