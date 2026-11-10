package com.mentesvivas.backend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class BodyMassIndex {

    private BodyMassIndex() {
    }

    public static BigDecimal calculate(BigDecimal weightKg, BigDecimal heightM) {
        if (weightKg == null || heightM == null || heightM.signum() <= 0) {
            return null;
        }
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }
}
