package com.mentesvivas.backend.service.medication;

import com.mentesvivas.backend.config.AdherenceProperties;
import com.mentesvivas.backend.dto.medication.AdherenceResponse;
import com.mentesvivas.backend.entity.medication.DoseRecord;
import com.mentesvivas.backend.entity.medication.DoseStatus;
import com.mentesvivas.backend.entity.medication.Medication;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AdherenceCalculator {

    private final AdherenceProperties properties;

    public AdherenceResponse calculate(List<Medication> medications, List<DoseRecord> doses, Instant from,
                                       Instant to, ZoneId zone) {
        int expected = medications.stream().mapToInt(medication -> expectedDoses(medication, from, to, zone)).sum();
        int taken = count(doses, DoseStatus.TAKEN);
        int refused = count(doses, DoseStatus.REFUSED);
        int postponed = count(doses, DoseStatus.POSTPONED);
        int notRegistered = Math.max(0, expected - taken - refused - postponed);
        BigDecimal percent = expected == 0 ? null : BigDecimal.valueOf(taken * 100L)
                .divide(BigDecimal.valueOf(expected), 1, RoundingMode.HALF_UP);
        boolean below = percent != null && percent.compareTo(BigDecimal.valueOf(properties.thresholdPercent())) < 0;
        return new AdherenceResponse(from, to, expected, taken, refused, postponed, notRegistered, percent,
                properties.thresholdPercent(), below);
    }

    private int count(List<DoseRecord> doses, DoseStatus status) {
        return (int) doses.stream().filter(dose -> dose.getStatus() == status).count();
    }

    private int expectedDoses(Medication medication, Instant from, Instant to, ZoneId zone) {
        Instant start = medication.getCreatedAt() != null && medication.getCreatedAt().isAfter(from)
                ? medication.getCreatedAt() : from;
        int total = 0;
        for (LocalDate day = start.atZone(zone).toLocalDate(); !day.isAfter(to.atZone(zone).toLocalDate());
             day = day.plusDays(1)) {
            for (LocalTime time : medication.getTimes()) {
                Instant slot = day.atTime(time).atZone(zone).toInstant();
                if (!slot.isBefore(start) && !slot.isAfter(to)) {
                    total++;
                }
            }
        }
        return total;
    }
}
