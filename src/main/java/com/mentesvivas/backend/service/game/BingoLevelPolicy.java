package com.mentesvivas.backend.service.game;

import org.springframework.stereotype.Component;

@Component
public class BingoLevelPolicy {

    public static final int MIN_LEVEL = 1;
    public static final int MAX_LEVEL = 3;
    private static final int SESSIONS_PER_LEVEL = 2;

    public int levelFor(long finishedSessions) {
        long level = MIN_LEVEL + Math.max(0, finishedSessions) / SESSIONS_PER_LEVEL;
        return (int) Math.min(MAX_LEVEL, level);
    }

    public int optionCount(int level) {
        return Math.max(MIN_LEVEL, Math.min(MAX_LEVEL, level)) + 1;
    }
}
