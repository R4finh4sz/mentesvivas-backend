package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.config.BingoProperties;
import com.mentesvivas.backend.dto.game.AnswerBingoRequest;
import com.mentesvivas.backend.dto.game.BingoAnswerResponse;
import com.mentesvivas.backend.dto.game.BingoRoundResponse;
import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSession;
import com.mentesvivas.backend.entity.game.BingoSound;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.repository.game.BingoRoundRepository;
import com.mentesvivas.backend.repository.game.BingoSessionRepository;
import com.mentesvivas.backend.repository.game.BingoSoundRepository;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BingoGameServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-24T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private BingoSoundRepository sounds;
    @Mock
    private BingoSessionRepository sessions;
    @Mock
    private BingoRoundRepository rounds;
    @Mock
    private BingoRoundFactory factory;
    @Mock
    private BingoRoundView view;

    private BingoGameService service;
    private BingoSession session;
    private List<BingoSound> library;

    @BeforeEach
    void setUp() {
        service = new BingoGameService(access, sounds, sessions, rounds, new BingoLevelPolicy(), factory, view,
                new BingoFeedback(), new BingoProperties(2, 5, 10), Clock.fixed(NOW, ZoneOffset.UTC));
        session = new BingoSession();
        session.setId(UUID.randomUUID());
        session.setPatientId(PATIENT);
        session.setTotalRounds(2);
        library = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            BingoSound sound = new BingoSound();
            sound.setId(UUID.randomUUID());
            sound.setInstrument("Instrumento " + i);
            library.add(sound);
        }
    }

    private BingoRound round(int position, BingoSound target) {
        BingoRound round = new BingoRound();
        round.setId(UUID.randomUUID());
        round.setSessionId(session.getId());
        round.setPosition(position);
        round.setSoundId(target.getId());
        round.defineOptions(library.subList(0, 3).stream().map(BingoSound::getId).toList());
        round.setShownAt(NOW.minusSeconds(8));
        return round;
    }

    private BingoRoundResponse response(BingoRound round) {
        return new BingoRoundResponse(round.getId(), round.getPosition(), 2, "https://s", 5, false, null, List.of());
    }

    private void currentIs(BingoRound round) {
        when(sessions.findByIdAndPatientId(session.getId(), PATIENT)).thenReturn(Optional.of(session));
        when(rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId()))
                .thenReturn(Optional.of(round));
        lenient().when(view.soundsOf(round)).thenReturn(library.stream()
                .collect(Collectors.toMap(BingoSound::getId, s -> s)));
    }

    @Test
    void startIsBlockedWithFewerThanTenSounds() {
        when(access.requireWriter(PATIENT)).thenReturn(new User());
        when(sounds.findByActiveTrueOrderByInstrumentAsc()).thenReturn(library.subList(0, 9));

        assertThatThrownBy(() -> service.start(PATIENT))
                .isInstanceOf(ConflictException.class).hasMessageContaining("10");
        verify(sessions, never()).save(any());
    }

    @Test
    void startUsesTheLevelFromFinishedSessionsToSetTheOptionCount() {
        User starter = new User();
        starter.setId(UUID.randomUUID());
        when(access.requireWriter(PATIENT)).thenReturn(starter);
        when(sounds.findByActiveTrueOrderByInstrumentAsc()).thenReturn(library);
        when(sessions.countByPatientIdAndFinishedAtIsNotNull(PATIENT)).thenReturn(2L);
        when(sessions.save(any(BingoSession.class))).thenAnswer(call -> {
            BingoSession saved = call.getArgument(0);
            saved.setId(session.getId());
            return saved;
        });
        BingoRound first = round(1, library.get(0));
        first.setShownAt(null);
        when(factory.build(session.getId(), library, 3, 2)).thenReturn(List.of(first));
        when(rounds.saveAll(anyList())).thenAnswer(call -> call.getArgument(0));
        when(view.of(first, 2, PATIENT)).thenReturn(response(first));

        var started = service.start(PATIENT);

        assertThat(started.sessionId()).isEqualTo(session.getId());
        assertThat(first.getShownAt()).isEqualTo(NOW);
    }

    @Test
    void wrongAnswerKeepsTheRoundAndCountsTheErrorInternally() {
        BingoRound current = round(1, library.get(0));
        currentIs(current);
        when(view.of(current, 2, PATIENT)).thenReturn(response(current));

        BingoAnswerResponse answer = service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(current.getId(), library.get(1).getId()));

        assertThat(answer.correct()).isFalse();
        assertThat(answer.message()).contains("tentar mais uma vez");
        assertThat(answer.finished()).isFalse();
        assertThat(current.isAnswered()).isFalse();
        assertThat(current.getAttempts()).isEqualTo(1);
        assertThat(session.getErrorCount()).isEqualTo(1);
    }

    @Test
    void correctAnswerMovesOnAndMeasuresTimeFromWhenTheOptionsAppear() {
        BingoRound current = round(1, library.get(0));
        BingoRound next = round(2, library.get(1));
        currentIs(current);
        when(rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId()))
                .thenReturn(Optional.of(current), Optional.of(next));
        when(view.of(next, 2, PATIENT)).thenReturn(response(next));

        BingoAnswerResponse answer = service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(current.getId(), library.get(0).getId()));

        assertThat(answer.correct()).isTrue();
        assertThat(answer.message()).contains("Instrumento 0");
        assertThat(answer.finished()).isFalse();
        assertThat(current.isAnswered()).isTrue();
        assertThat(current.getResponseMs()).isEqualTo(3000L);
        assertThat(next.getShownAt()).isEqualTo(NOW);
    }

    @Test
    void responseTimeNeverGoesNegative() {
        BingoRound current = round(1, library.get(0));
        current.setShownAt(NOW.minusSeconds(1));
        currentIs(current);
        when(rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId()))
                .thenReturn(Optional.of(current), Optional.empty());

        service.answer(PATIENT, session.getId(), new AnswerBingoRequest(current.getId(), library.get(0).getId()));

        assertThat(current.getResponseMs()).isZero();
    }

    @Test
    void lastCorrectAnswerConcludesTheGame() {
        BingoRound current = round(2, library.get(0));
        currentIs(current);
        when(rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId()))
                .thenReturn(Optional.of(current), Optional.empty());

        BingoAnswerResponse answer = service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(current.getId(), library.get(0).getId()));

        assertThat(answer.finished()).isTrue();
        assertThat(answer.round()).isNull();
        assertThat(answer.message()).contains("concluiu");
        assertThat(session.getFinishedAt()).isEqualTo(NOW);
    }

    @Test
    void answerForAnotherRoundIsRejected() {
        currentIs(round(1, library.get(0)));

        assertThatThrownBy(() -> service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(UUID.randomUUID(), library.get(0).getId())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void answerOutsideTheOptionsIsRejected() {
        BingoRound current = round(1, library.get(0));
        currentIs(current);

        assertThatThrownBy(() -> service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(current.getId(), library.get(9).getId())))
                .isInstanceOf(ImpossibleValueException.class);
    }

    @Test
    void finishedGameCannotBeAnswered() {
        session.setFinishedAt(NOW);
        when(sessions.findByIdAndPatientId(session.getId(), PATIENT)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> service.answer(PATIENT, session.getId(),
                new AnswerBingoRequest(UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void unknownSessionIsNotFound() {
        when(sessions.findByIdAndPatientId(session.getId(), PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.current(PATIENT, session.getId()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void currentMarksTheRoundAsShownOnlyOnce() {
        BingoRound current = round(1, library.get(0));
        current.setShownAt(null);
        currentIs(current);
        when(view.of(current, 2, PATIENT)).thenReturn(response(current));

        service.current(PATIENT, session.getId());

        assertThat(current.getShownAt()).isEqualTo(NOW);
    }
}
