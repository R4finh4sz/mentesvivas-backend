package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.entity.game.BingoRound;
import com.mentesvivas.backend.entity.game.BingoSound;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.random.RandomGenerator;

@Component
public class BingoRoundFactory {

    private final RandomGenerator random;

    @Autowired
    public BingoRoundFactory() {
        this(new SecureRandom());
    }

    BingoRoundFactory(RandomGenerator random) {
        this.random = random;
    }

    public List<BingoRound> build(UUID sessionId, List<BingoSound> sounds, int optionCount, int total) {
        List<BingoSound> targets = new ArrayList<>(sounds);
        shuffle(targets);

        List<BingoRound> rounds = new ArrayList<>();
        for (int position = 0; position < total; position++) {
            BingoSound target = targets.get(position % targets.size());
            rounds.add(round(sessionId, position + 1, target, sounds, optionCount));
        }
        return rounds;
    }

    private BingoRound round(UUID sessionId, int position, BingoSound target, List<BingoSound> sounds,
                             int optionCount) {
        List<BingoSound> others = new ArrayList<>(sounds.stream()
                .filter(sound -> !sound.getId().equals(target.getId()))
                .toList());
        shuffle(others);
        List<BingoSound> options = new ArrayList<>(others.subList(0, Math.min(optionCount - 1, others.size())));
        options.add(target);
        shuffle(options);

        BingoRound round = new BingoRound();
        round.setSessionId(sessionId);
        round.setPosition(position);
        round.setSoundId(target.getId());
        round.defineOptions(options.stream().map(BingoSound::getId).toList());
        return round;
    }

    private <T> void shuffle(List<T> items) {
        for (int i = items.size() - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            T swap = items.get(i);
            items.set(i, items.get(j));
            items.set(j, swap);
        }
    }
}
