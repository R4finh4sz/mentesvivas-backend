package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.BingoRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BingoRoundRepository extends JpaRepository<BingoRound, UUID> {

    Optional<BingoRound> findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(UUID sessionId);
}
