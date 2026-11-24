package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.dto.game.GamePerformanceMetrics;
import com.mentesvivas.backend.entity.game.RecognitionSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RecognitionSessionRepository extends JpaRepository<RecognitionSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecognitionSession> findByIdAndPatientId(UUID id, UUID patientId);

    boolean existsByPatientIdAndFinishedAtIsNotNull(UUID patientId);

    @Query("""
            select new com.mentesvivas.backend.dto.game.GamePerformanceMetrics(
                count(distinct session.id), count(question.memberId),
                coalesce(sum(question.errors), 0), avg(question.responseMs))
            from RecognitionSession session join session.questions question
            where session.patientId = :patientId and session.finishedAt is not null
            """)
    GamePerformanceMetrics performance(@Param("patientId") UUID patientId);
}
