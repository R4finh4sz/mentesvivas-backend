package com.mentesvivas.backend.entity.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bingo_sessions")
@Getter
@Setter
@NoArgsConstructor
public class BingoSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID startedBy;

    @Column(nullable = false)
    private int difficultyLevel;

    @Column(nullable = false)
    private int totalRounds;

    @Column(nullable = false)
    private int errorCount;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    public boolean isFinished() {
        return finishedAt != null;
    }
}
