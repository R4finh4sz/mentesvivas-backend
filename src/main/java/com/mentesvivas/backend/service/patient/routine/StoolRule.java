package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class StoolRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.STOOL;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.range(request.stoolFrequency(), 0, 30, "Frequencia das fezes");
        RoutineChecks.range(request.bristolScale(), 1, 7, "Escala de Bristol");
    }
}
