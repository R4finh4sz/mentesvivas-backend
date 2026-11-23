package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Component
public class RoutineValidator {

    private final Map<RoutineType, RoutineRule> rules = new EnumMap<>(RoutineType.class);

    public RoutineValidator(List<RoutineRule> available) {
        available.forEach(rule -> rules.put(rule.type(), rule));
    }

    public void validate(RecordRoutineRequest request) {
        rules.get(request.type()).validate(request);
    }
}
