package com.mentesvivas.backend.entity.alert;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SosAlertTest {

    private static final Instant CREATED = Instant.parse("2026-11-23T11:00:00Z");
    private static final Instant LATER = Instant.parse("2026-11-23T11:05:00Z");

    private final UUID actor = UUID.randomUUID();

    @Test
    void startsCreated() {
        SosAlert alert = new SosAlert(UUID.randomUUID(), CREATED);

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.CREATED);
        assertThat(alert.getStatus().isPending()).isTrue();
    }

    @Test
    void advancesStepByStepKeepingEachAuthorAndTime() {
        SosAlert alert = new SosAlert(UUID.randomUUID(), CREATED);
        UUID other = UUID.randomUUID();

        assertThat(alert.advance(AlertStatus.SEEN, actor, CREATED)).isTrue();
        assertThat(alert.advance(AlertStatus.ACKNOWLEDGED, other, LATER)).isTrue();

        assertThat(alert.getSeenBy()).isEqualTo(actor);
        assertThat(alert.getSeenAt()).isEqualTo(CREATED);
        assertThat(alert.getAcknowledgedBy()).isEqualTo(other);
        assertThat(alert.getAcknowledgedAt()).isEqualTo(LATER);
    }

    @Test
    void jumpingToResolvedFillsEveryIntermediateStep() {
        SosAlert alert = new SosAlert(UUID.randomUUID(), CREATED);

        assertThat(alert.advance(AlertStatus.RESOLVED, actor, LATER)).isTrue();

        assertThat(alert.getSeenAt()).isEqualTo(LATER);
        assertThat(alert.getAcknowledgedAt()).isEqualTo(LATER);
        assertThat(alert.getResolvedAt()).isEqualTo(LATER);
        assertThat(alert.getStatus().isPending()).isFalse();
    }

    @Test
    void neverMovesBackwardsNorRepeatsAndKeepsTheOriginalAuthor() {
        SosAlert alert = new SosAlert(UUID.randomUUID(), CREATED);
        alert.advance(AlertStatus.ACKNOWLEDGED, actor, CREATED);

        assertThat(alert.advance(AlertStatus.SEEN, UUID.randomUUID(), LATER)).isFalse();
        assertThat(alert.advance(AlertStatus.ACKNOWLEDGED, UUID.randomUUID(), LATER)).isFalse();

        assertThat(alert.getStatus()).isEqualTo(AlertStatus.ACKNOWLEDGED);
        assertThat(alert.getAcknowledgedBy()).isEqualTo(actor);
        assertThat(alert.getAcknowledgedAt()).isEqualTo(CREATED);
    }
}
