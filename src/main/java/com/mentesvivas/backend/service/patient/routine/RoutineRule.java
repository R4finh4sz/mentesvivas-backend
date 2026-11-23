package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;

public interface RoutineRule {

    RoutineType type();

    void validate(RecordRoutineRequest request);
}
