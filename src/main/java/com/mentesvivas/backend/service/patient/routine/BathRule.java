package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class BathRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.BATH;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.required(request.bathCooperation(), "a cooperacao no banho");
    }
}
