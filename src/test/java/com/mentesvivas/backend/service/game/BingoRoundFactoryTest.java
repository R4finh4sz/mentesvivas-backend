package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSound;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class BingoRoundFactoryTest {

    private final BingoRoundFactory factory = new BingoRoundFactory(new Random(11));

    private List<BingoSound> library(int size) {
        List<BingoSound> sounds = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            BingoSound sound = new BingoSound();
            sound.setId(UUID.randomUUID());
            sound.setInstrument("Instrumento " + i);
            sounds.add(sound);
        }
        return sounds;
    }

    @Test
    void buildsTheRequestedRoundsInOrder() {
        List<BingoRound> rounds = factory.build(UUID.randomUUID(), library(10), 3, 6);

        assertThat(rounds).extracting(BingoRound::getPosition).containsExactly(1, 2, 3, 4, 5, 6);
    }

    @Test
    void eachRoundHasTheRequestedNumberOfDistinctOptionsWithTheSound() {
        for (int count = 2; count <= 4; count++) {
            for (BingoRound round : factory.build(UUID.randomUUID(), library(10), count, 5)) {
                assertThat(round.options()).hasSize(count).doesNotHaveDuplicates().contains(round.getSoundId());
            }
        }
    }

    @Test
    void soundsDoNotRepeatBeforeTheWholeLibraryWasUsed() {
        List<BingoRound> rounds = factory.build(UUID.randomUUID(), library(10), 3, 10);

        assertThat(rounds).extracting(BingoRound::getSoundId).doesNotHaveDuplicates();
    }

    @Test
    void roundsStartUnansweredAndNotShown() {
        BingoRound round = factory.build(UUID.randomUUID(), library(10), 2, 1).get(0);

        assertThat(round.isAnswered()).isFalse();
        assertThat(round.getShownAt()).isNull();
        assertThat(round.getAttempts()).isZero();
    }

    @Test
    void defaultConstructorWorks() {
        assertThat(new BingoRoundFactory().build(UUID.randomUUID(), library(10), 4, 2)).hasSize(2);
    }
}
