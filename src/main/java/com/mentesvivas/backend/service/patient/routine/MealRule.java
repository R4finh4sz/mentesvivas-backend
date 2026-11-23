package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class MealRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.MEAL;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.required(request.mealPeriod(), "o periodo da refeicao");
        RoutineChecks.required(request.mealAccepted(), "se a refeicao foi aceita ou recusada");
    }
}
