package com.mentesvivas.backend.dto.clinical;

import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.OccurrenceResponse;
import com.mentesvivas.backend.dto.patient.RoutineRecordResponse;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;

import java.time.Instant;
import java.util.List;

public record TimelineResponse(
        Instant from,
        Instant to,
        boolean hasData,
        List<TimelineCategory> categoriesWithoutData,
        List<VitalSignResponse> vitalSigns,
        List<RoutineRecordResponse> moods,
        List<RoutineRecordResponse> sleeps,
        List<OccurrenceResponse> occurrences,
        List<MeasurementResponse> weights,
        List<TimelineDose> medicationDoses
) {
}
