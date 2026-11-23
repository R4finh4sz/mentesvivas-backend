package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

@Component
public class HydrationRule implements RoutineRule {

    @Override
    public RoutineType type() {
        return RoutineType.HYDRATION;
    }

    @Override
    public void validate(RecordRoutineRequest request) {
        RoutineChecks.range(request.amountMl(), 1, 5000, "Quantidade em ml");
    }
}
