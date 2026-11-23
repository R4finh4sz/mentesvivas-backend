package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class MoodRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.MOOD;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.required(request.mood(), "o humor");
    }
}
