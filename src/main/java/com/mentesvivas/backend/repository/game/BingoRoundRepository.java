package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.dto.game.GamePerformanceMetrics;
import com.mentesvivas.backend.entity.game.BingoRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface BingoRoundRepository extends JpaRepository<BingoRound, UUID> {

    Optional<BingoRound> findFirstBySessionIdAndAnsweredAtIsNullOrderByPositionAsc(UUID sessionId);

    @Query("""
            select new com.mentesvivas.backend.dto.game.GamePerformanceMetrics(
                count(distinct session.id), count(round), coalesce(sum(round.attempts - 1), 0), avg(round.responseMs))
            from BingoRound round join BingoSession session on session.id = round.sessionId
            where session.patientId = :patientId and session.finishedAt is not null
              and round.answeredAt is not null
            """)
    GamePerformanceMetrics performance(@Param("patientId") UUID patientId);
}
