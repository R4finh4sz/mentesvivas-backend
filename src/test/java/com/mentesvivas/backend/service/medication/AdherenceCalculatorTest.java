package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.AdherenceProperties;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class AdherenceCalculatorTest {

    private static final Instant TO = Instant.parse("2026-11-30T12:00:00Z");
    private static final Instant FROM = TO.minusSeconds(10L * 24 * 3600);

    private final AdherenceCalculator calculator = new AdherenceCalculator(new AdherenceProperties(30, 70));

    private Medication medication(Instant createdAt, LocalTime... times) {
        Medication medication = new Medication();
        medication.setCreatedAt(createdAt);
        medication.setTimes(List.of(times));
        return medication;
    }

    private List<DoseRecord> doses(DoseStatus status, int amount) {
        return IntStream.range(0, amount).mapToObj(i -> {
            DoseRecord dose = new DoseRecord();
            dose.setStatus(status);
            return dose;
        }).toList();
    }

    @Test
    void expectedDosesFollowTheScheduleInsideThePeriod() {
        var result = calculator.calculate(List.of(medication(FROM.minusSeconds(86400), LocalTime.of(8, 0),
                LocalTime.of(20, 0))), List.of(), FROM, TO, ZoneOffset.UTC);

        assertThat(result.expectedDoses()).isEqualTo(20);
        assertThat(result.notRegistered()).isEqualTo(20);
        assertThat(result.adherencePercent()).isEqualByComparingTo("0");
        assertThat(result.belowThreshold()).isTrue();
    }

    @Test
    void medicationCreatedInsideThePeriodOnlyCountsFromCreation() {
        Instant created = TO.minusSeconds(2L * 24 * 3600).minusSeconds(3600);

        var result = calculator.calculate(List.of(medication(created, LocalTime.of(8, 0))), List.of(), FROM, TO,
                ZoneOffset.UTC);

        assertThat(result.expectedDoses()).isEqualTo(2);
    }

    @Test
    void adherenceIsTakenOverExpectedAndMissingRecordsAreNotTaken() {
        var all = new java.util.ArrayList<>(doses(DoseStatus.TAKEN, 14));
        all.addAll(doses(DoseStatus.REFUSED, 2));
        all.addAll(doses(DoseStatus.POSTPONED, 1));

        var result = calculator.calculate(List.of(medication(FROM.minusSeconds(86400), LocalTime.of(8, 0),
                LocalTime.of(20, 0))), all, FROM, TO, ZoneOffset.UTC);

        assertThat(result.taken()).isEqualTo(14);
        assertThat(result.refused()).isEqualTo(2);
        assertThat(result.postponed()).isEqualTo(1);
        assertThat(result.notRegistered()).isEqualTo(3);
        assertThat(result.adherencePercent()).isEqualByComparingTo("70.0");
        assertThat(result.belowThreshold()).isFalse();
    }

    @Test
    void justBelowTheThresholdIsFlagged() {
        var result = calculator.calculate(List.of(medication(FROM.minusSeconds(86400), LocalTime.of(8, 0),
                LocalTime.of(20, 0))), doses(DoseStatus.TAKEN, 13), FROM, TO, ZoneOffset.UTC);

        assertThat(result.adherencePercent()).isEqualByComparingTo("65.0");
        assertThat(result.belowThreshold()).isTrue();
    }

    @Test
    void noExpectedDosesMeansNoPercentageAndNoAlert() {
        var result = calculator.calculate(List.of(), List.of(), FROM, TO, ZoneOffset.UTC);

        assertThat(result.expectedDoses()).isZero();
        assertThat(result.adherencePercent()).isNull();
        assertThat(result.belowThreshold()).isFalse();
    }

    @Test
    void notRegisteredNeverGoesNegative() {
        var result = calculator.calculate(List.of(medication(FROM.minusSeconds(86400), LocalTime.of(8, 0))),
                doses(DoseStatus.TAKEN, 50), FROM, TO, ZoneOffset.UTC);

        assertThat(result.notRegistered()).isZero();
    }
}
