package com.mentesvivas.backend.service.patient.hydration;

import com.mentesvivas.backend.config.HydrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
@RequiredArgsConstructor
public class HydrationGoalCalculator {

    private final HydrationProperties properties;
    private final HydrationConditionClassifier classifier;

    public HydrationGoal calculate(String declaredDisease, Integer doctorGoalMl, BigDecimal weightKg) {
        if (doctorGoalMl != null) {
            return new HydrationGoal(HydrationGoalStatus.DOCTOR_DEFINED, doctorGoalMl, doctorGoalMl, false);
        }
        HydrationCondition condition = classifier.classify(declaredDisease);
        if (condition == HydrationCondition.MEDICAL_GOAL_REQUIRED) {
            return new HydrationGoal(HydrationGoalStatus.DOCTOR_GOAL_REQUIRED, null, null, false);
        }
        if (weightKg == null) {
            return new HydrationGoal(HydrationGoalStatus.WEIGHT_MISSING, null, null, false);
        }
        return new HydrationGoal(HydrationGoalStatus.FORMULA, perKg(weightKg, properties.mlPerKgMin()),
                perKg(weightKg, properties.mlPerKgMax()), condition == HydrationCondition.CONSULT_DOCTOR);
    }

    private int perKg(BigDecimal weightKg, BigDecimal factor) {
        return weightKg.multiply(factor).setScale(0, RoundingMode.HALF_UP).intValue();
    }
}
