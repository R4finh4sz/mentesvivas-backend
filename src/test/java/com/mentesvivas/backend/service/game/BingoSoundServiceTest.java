package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.SaveBingoSoundRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.game.BingoSound;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.game.BingoSoundRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BingoSoundServiceTest {

    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    @Mock
    private BingoSoundRepository repository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditTrail auditTrail;

    private BingoSoundService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new BingoSoundService(repository, currentUser, auditTrail, Clock.fixed(NOW, ZoneOffset.UTC));
        admin = new User();
        admin.setId(UUID.randomUUID());
    }

    @Test
    void createsASoundWithNormalizedKeyAndRecordsTheAudit() {
        when(currentUser.get()).thenReturn(admin);
        when(repository.save(any(BingoSound.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(new SaveBingoSoundRequest("  Violão  ", " https://audio/violao.mp3 ", null));

        ArgumentCaptor<BingoSound> saved = ArgumentCaptor.forClass(BingoSound.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getInstrument()).isEqualTo("Violão");
        assertThat(saved.getValue().getSearchKey()).isEqualTo("violao");
        assertThat(saved.getValue().getAudioUrl()).isEqualTo("https://audio/violao.mp3");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.instrument()).isEqualTo("Violão");

        ArgumentCaptor<AuditRecord> audit = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(audit.capture());
        assertThat(audit.getValue().type()).isEqualTo(AuditEventType.BINGO_SOUND_CHANGED);
        assertThat(audit.getValue().actorUserId()).isEqualTo(admin.getId());
    }

    @Test
    void rejectsADuplicatedInstrument() {
        when(repository.existsBySearchKey("violao")).thenReturn(true);

        assertThatThrownBy(() -> service.create(new SaveBingoSoundRequest("VIOLÃO", "https://a/b.mp3", true)))
                .isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updatesAndCanDeactivateASound() {
        BingoSound existing = new BingoSound();
        existing.setId(UUID.randomUUID());
        existing.setActive(true);
        when(currentUser.get()).thenReturn(admin);
        when(repository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        var response = service.update(existing.getId(), new SaveBingoSoundRequest("Flauta", "https://a/f.mp3", false));

        assertThat(response.active()).isFalse();
        assertThat(existing.getInstrument()).isEqualTo("Flauta");
    }

    @Test
    void updatingAnUnknownSoundFails() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, new SaveBingoSoundRequest("Flauta", "https://a/f.mp3", true)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatingToAnotherSoundsNameIsAConflict() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new BingoSound()));
        when(repository.existsBySearchKeyAndIdNot("flauta", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, new SaveBingoSoundRequest("Flauta", "https://a/f.mp3", true)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void listsTheLibrary() {
        BingoSound sound = new BingoSound();
        sound.setId(UUID.randomUUID());
        sound.setInstrument("Piano");
        when(repository.findAllByOrderByInstrumentAsc()).thenReturn(List.of(sound));

        assertThat(service.list()).extracting("instrument").containsExactly("Piano");
    }
}
