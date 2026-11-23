package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class UrineRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.URINE;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.range(request.urineFrequency(), 0, 30, "Frequencia da urina");
        RoutineChecks.required(request.urineAspect(), "o aspecto da urina");
    }
}
