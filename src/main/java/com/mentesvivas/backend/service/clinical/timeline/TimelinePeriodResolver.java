package com.mentesvivas.backend.service.clinical.timeline;

import com.mentesvivas.backend.config.TimelineProperties;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

@Component
@RequiredArgsConstructor
public class TimelinePeriodResolver {

    private final TimelineProperties properties;
    private final Clock clock;

    public TimelinePeriod resolve(Instant from, Instant to) {
        Instant end = to == null ? clock.instant() : to;
        Instant start = from == null ? end.minus(Duration.ofDays(properties.defaultDays())) : from;
        if (start.isAfter(end)) {
            throw new ImpossibleValueException("O inicio do periodo nao pode ser depois do fim.");
        }
        if (Duration.between(start, end).compareTo(Duration.ofDays(properties.maxDays())) > 0) {
            throw new ImpossibleValueException("O periodo aceita no maximo %d dias.".formatted(properties.maxDays()));
        }
        return new TimelinePeriod(start, end);
    }
}
