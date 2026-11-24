package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.RecognitionSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface RecognitionSessionRepository extends JpaRepository<RecognitionSession, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RecognitionSession> findByIdAndPatientId(UUID id, UUID patientId);

    boolean existsByPatientIdAndFinishedAtIsNotNull(UUID patientId);
}
