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
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "bingo_rounds")
@Getter
@Setter
@NoArgsConstructor
public class BingoRound {

    private static final String SEPARATOR = ",";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID sessionId;

    @Column(nullable = false)
    private int position;

    @Column(nullable = false)
    private UUID soundId;

    @Column(nullable = false, length = 200)
    private String optionIds;

    @Column(nullable = false)
    private int attempts;

    private Instant shownAt;

    private Instant answeredAt;

    private Long responseMs;

    public List<UUID> options() {
        return Arrays.stream(optionIds.split(SEPARATOR)).map(UUID::fromString).toList();
    }

    public void defineOptions(List<UUID> options) {
        this.optionIds = String.join(SEPARATOR, options.stream().map(UUID::toString).toList());
    }

    public boolean isAnswered() {
        return answeredAt != null;
    }
}
