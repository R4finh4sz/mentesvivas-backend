package com.mentesvivas.backend.entity.game;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BingoRoundTest {

    @Test
    void optionsSurviveTheRoundTrip() {
        List<UUID> ids = List.of(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        BingoRound round = new BingoRound();

        round.defineOptions(ids);

        assertThat(round.options()).containsExactlyElementsOf(ids);
        assertThat(round.getOptionIds().length()).isLessThanOrEqualTo(200);
    }

    @Test
    void isAnsweredOnlyAfterTheAnswerTime() {
        BingoRound round = new BingoRound();
        assertThat(round.isAnswered()).isFalse();

        round.setAnsweredAt(Instant.now());

        assertThat(round.isAnswered()).isTrue();
    }

    @Test
    void sessionIsFinishedOnlyAfterTheFinishTime() {
        BingoSession session = new BingoSession();
        assertThat(session.isFinished()).isFalse();

        session.setFinishedAt(Instant.now());

        assertThat(session.isFinished()).isTrue();
    }
}
