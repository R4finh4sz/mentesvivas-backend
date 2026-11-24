package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.BingoSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface BingoSessionRepository extends JpaRepository<BingoSession, UUID> {

    Optional<BingoSession> findByIdAndPatientId(UUID id, UUID patientId);

    long countByPatientIdAndFinishedAtIsNotNull(UUID patientId);
}
