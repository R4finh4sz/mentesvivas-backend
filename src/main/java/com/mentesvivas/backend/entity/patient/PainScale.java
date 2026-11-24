package com.mentesvivas.backend.entity.patient;

import java.util.List;
import java.util.stream.IntStream;

public enum PainScale {
    NUMERIC(IntStream.rangeClosed(0, 10).boxed().toList()),
    WONG_BAKER(List.of(0, 2, 4, 6, 8, 10)),
    PAINAD(IntStream.rangeClosed(0, 10).boxed().toList()),
    DOLOPLUS_2(IntStream.rangeClosed(0, 30).boxed().toList());

    public static final PainScale DEFAULT = NUMERIC;

    private final List<Integer> scores;

    PainScale(List<Integer> scores) {
        this.scores = scores;
    }

    public List<Integer> scores() {
        return scores;
    }
}
