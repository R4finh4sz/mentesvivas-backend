package com.mentesvivas.backend.entity.game;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecognitionQuestion {

    @Column(nullable = false)
    private UUID memberId;

    @Column(nullable = false, length = 200)
    private String optionIds;

    @Column(nullable = false)
    private int errors;

    private Long responseMs;

    public RecognitionQuestion(UUID memberId, List<UUID> options) {
        this.memberId = memberId;
        this.optionIds = String.join(",", options.stream().map(UUID::toString).toList());
    }

    public List<UUID> options() {
        return Arrays.stream(optionIds.split(",")).map(UUID::fromString).toList();
    }

    public void recordError() {
        errors++;
    }

    public void recordAnswer(long responseMs) {
        this.responseMs = responseMs;
    }
}
