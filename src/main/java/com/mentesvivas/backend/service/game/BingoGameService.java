package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.config.BingoProperties;
import com.mentesvivas.backend.dto.game.AnswerBingoRequest;
import com.mentesvivas.backend.dto.game.BingoAnswerResponse;
import com.mentesvivas.backend.dto.game.BingoRoundResponse;
import com.mentesvivas.backend.dto.game.StartBingoResponse;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BingoGameService {

    private static final String FINISHED = "Este jogo ja foi concluido. Volte a lista de jogos.";

    private final PatientCareAccess access;
    private final BingoSoundRepository sounds;
    private final BingoSessionRepository sessions;
    private final BingoRoundRepository rounds;
    private final BingoLevelPolicy levels;
    private final BingoRoundFactory factory;
    private final BingoRoundView view;
    private final BingoFeedback feedback;
    private final BingoProperties properties;
    private final Clock clock;

    @Transactional
    public StartBingoResponse start(UUID patientId) {
        User starter = access.requireWriter(patientId);
        List<BingoSound> library = sounds.findByActiveTrueOrderByInstrumentAsc();
        if (library.size() < properties.minimumSounds()) {
            throw new ConflictException("O Bingo Musical ainda nao esta disponivel: a biblioteca precisa de pelo menos "
                    + properties.minimumSounds() + " sons.");
        }

        Instant now = clock.instant();
        BingoSession session = new BingoSession();
        session.setPatientId(patientId);
        session.setStartedBy(starter.getId());
        session.setDifficultyLevel(levels.levelFor(sessions.countByPatientIdAndFinishedAtIsNotNull(patientId)));
        session.setTotalRounds(properties.roundsPerSession());
        session.setStartedAt(now);
        sessions.save(session);

        List<BingoRound> created = rounds.saveAll(factory.build(session.getId(), library,
                levels.optionCount(session.getDifficultyLevel()), session.getTotalRounds()));
        BingoRound first = created.get(0);
        first.setShownAt(now);
        return new StartBingoResponse(session.getId(), session.getTotalRounds(),
                view.of(first, session.getTotalRounds(), patientId));
    }

    @Transactional
    public BingoRoundResponse current(UUID patientId, UUID sessionId) {
        access.requireWriter(patientId);
        BingoSession session = openSession(patientId, sessionId);
        BingoRound round = currentRound(session);
        markShown(round);
        return view.of(round, session.getTotalRounds(), patientId);
    }

    @Transactional
    public BingoAnswerResponse answer(UUID patientId, UUID sessionId, AnswerBingoRequest request) {
        access.requireWriter(patientId);
        BingoSession session = openSession(patientId, sessionId);
        BingoRound round = currentRound(session);
        if (!round.getId().equals(request.roundId())) {
            throw new ConflictException("Esta nao e a rodada atual do jogo.");
        }
        if (!round.options().contains(request.soundId())) {
            throw new ImpossibleValueException("O instrumento escolhido nao esta entre as opcoes da rodada.");
        }

        Instant now = clock.instant();
        markShown(round);
        round.setAttempts(round.getAttempts() + 1);

        if (!round.getSoundId().equals(request.soundId())) {
            session.setErrorCount(session.getErrorCount() + 1);
            return new BingoAnswerResponse(false, feedback.retry(), false,
                    view.of(round, session.getTotalRounds(), patientId));
        }

        String instrument = view.soundsOf(round).get(round.getSoundId()).getInstrument();
        round.setAnsweredAt(now);
        round.setResponseMs(responseMs(round, now));
        return rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId())
                .map(next -> next(session, next, instrument, now, patientId))
                .orElseGet(() -> conclude(session, now));
    }

    private BingoAnswerResponse next(BingoSession session, BingoRound next, String instrument, Instant now,
                                     UUID patientId) {
        next.setShownAt(now);
        return new BingoAnswerResponse(true, feedback.hit(instrument), false,
                view.of(next, session.getTotalRounds(), patientId));
    }

    private BingoAnswerResponse conclude(BingoSession session, Instant now) {
        session.setFinishedAt(now);
        return new BingoAnswerResponse(true, feedback.completion(), true, null);
    }

    private long responseMs(BingoRound round, Instant answeredAt) {
        Instant optionsAppear = round.getShownAt().plusSeconds(properties.soundSeconds());
        return Math.max(0, Duration.between(optionsAppear, answeredAt).toMillis());
    }

    private BingoSession openSession(UUID patientId, UUID sessionId) {
        BingoSession session = sessions.findByIdAndPatientId(sessionId, patientId)
                .orElseThrow(() -> new ResourceNotFoundException("Jogo nao encontrado."));
        if (session.isFinished()) {
            throw new ConflictException(FINISHED);
        }
        return session;
    }

    private BingoRound currentRound(BingoSession session) {
        return rounds.findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(session.getId())
                .orElseThrow(() -> new ConflictException(FINISHED));
    }

    private void markShown(BingoRound round) {
        if (round.getShownAt() == null) {
            round.setShownAt(clock.instant());
        }
    }
}
