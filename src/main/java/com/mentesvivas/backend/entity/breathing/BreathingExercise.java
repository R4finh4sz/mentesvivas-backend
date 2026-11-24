package com.mentesvivas.backend.entity.breathing;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "breathing_exercises")
@Getter
@NoArgsConstructor
public class BreathingExercise {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false)
    private int inhaleSeconds;

    @Column(nullable = false)
    private int holdSeconds;

    @Column(nullable = false)
    private int exhaleSeconds;

    @Column(nullable = false)
    private int cycles;

    @Column(nullable = false)
    private UUID validatedBy;

    @Column(nullable = false)
    private Instant validatedAt;

    @Column(nullable = false)
    private boolean published;

    public BreathingExercise(String title, int inhaleSeconds, int holdSeconds, int exhaleSeconds, int cycles,
                             UUID validatedBy, Instant validatedAt) {
        this.title = title;
        this.inhaleSeconds = inhaleSeconds;
        this.holdSeconds = holdSeconds;
        this.exhaleSeconds = exhaleSeconds;
        this.cycles = cycles;
        this.validatedBy = validatedBy;
        this.validatedAt = validatedAt;
    }

    public void setPublished(boolean published) {
        this.published = published;
    }
}
