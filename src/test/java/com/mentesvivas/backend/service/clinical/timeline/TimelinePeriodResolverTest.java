package com.mentesvivas.backend.service.clinical.timeline;

import com.mentesvivas.backend.config.TimelineProperties;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TimelinePeriodResolverTest {

    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    private final TimelinePeriodResolver resolver =
            new TimelinePeriodResolver(new TimelineProperties(7, 90), Clock.fixed(NOW, ZoneOffset.UTC));

    @Test
    void defaultsToTheLastConfiguredDays() {
        TimelinePeriod period = resolver.resolve(null, null);

        assertThat(period.to()).isEqualTo(NOW);
        assertThat(period.from()).isEqualTo(Instant.parse("2026-11-16T12:00:00Z"));
    }

    @Test
    void usesTheInformedPeriod() {
        Instant from = Instant.parse("2026-10-01T00:00:00Z");
        Instant to = Instant.parse("2026-10-31T00:00:00Z");

        assertThat(resolver.resolve(from, to)).isEqualTo(new TimelinePeriod(from, to));
    }

    @Test
    void onlyStartEndsNow() {
        Instant from = Instant.parse("2026-11-01T00:00:00Z");

        assertThat(resolver.resolve(from, null)).isEqualTo(new TimelinePeriod(from, NOW));
    }

    @Test
    void onlyEndStartsTheDefaultWindowBefore() {
        Instant to = Instant.parse("2026-11-10T00:00:00Z");

        assertThat(resolver.resolve(null, to).from()).isEqualTo(Instant.parse("2026-11-03T00:00:00Z"));
    }

    @Test
    void startAfterEndIsRejected() {
        assertThatThrownBy(() -> resolver.resolve(NOW, NOW.minusSeconds(1)))
                .isInstanceOf(ImpossibleValueException.class);
    }

    @Test
    void periodLongerThanTheLimitIsRejected() {
        assertThatThrownBy(() -> resolver.resolve(NOW.minusSeconds(91L * 24 * 3600), NOW))
                .isInstanceOf(ImpossibleValueException.class);
        assertThat(resolver.resolve(NOW.minusSeconds(90L * 24 * 3600), NOW)).isNotNull();
    }
}
