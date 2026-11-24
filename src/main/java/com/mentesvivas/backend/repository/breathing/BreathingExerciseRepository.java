package com.mentesvivas.backend.repository.breathing;

import com.mentesvivas.backend.entity.breathing.BreathingExercise;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface BreathingExerciseRepository extends JpaRepository<BreathingExercise, UUID> {

    List<BreathingExercise> findAllByOrderByTitleAscIdAsc();

    List<BreathingExercise> findByPublishedTrueOrderByTitleAscIdAsc();

    Optional<BreathingExercise> findByIdAndPublishedTrue(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<BreathingExercise> findExerciseById(UUID id);
}
