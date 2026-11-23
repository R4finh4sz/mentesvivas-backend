package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.RoutineType;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RoutineValidatorTest {

    private RoutineRule rule(RoutineType type) {
        RoutineRule rule = mock(RoutineRule.class);
        when(rule.type()).thenReturn(type);
        return rule;
    }

    @Test
    void delegatesOnlyToTheRuleOfTheRequestedType() {
        RoutineRule mood = rule(RoutineType.MOOD);
        RoutineRule sleep = rule(RoutineType.SLEEP);
        RoutineValidator validator = new RoutineValidator(List.of(mood, sleep));
        RecordRoutineRequest request = new RecordRoutineRequest(RoutineType.SLEEP, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null);

        validator.validate(request);

        verify(sleep).validate(request);
        verify(mood, never()).validate(request);
    }
}
