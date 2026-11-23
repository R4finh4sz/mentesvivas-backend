package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class SleepRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.SLEEP;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.required(request.sleepQuality(), "a qualidade do sono");
        RoutineChecks.range(request.sleepHours(), BigDecimal.ZERO, new BigDecimal("24"), "Horas de sono");
    }
}
