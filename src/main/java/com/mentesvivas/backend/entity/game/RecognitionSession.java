package com.mentesvivas.backend.entity.game;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "recognition_sessions")
@Getter
@Setter
@NoArgsConstructor
public class RecognitionSession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID patientId;

    @Column(nullable = false)
    private UUID startedBy;

    @Column(nullable = false)
    private Instant startedAt;

    private Instant finishedAt;

    @Column(nullable = false)
    private int questionIndex;

    @Column(nullable = false)
    private Instant questionShownAt;

    @ElementCollection
    @CollectionTable(name = "recognition_questions", joinColumns = @JoinColumn(name = "session_id"))
    @OrderColumn(name = "position")
    private List<RecognitionQuestion> questions = new ArrayList<>();
}
