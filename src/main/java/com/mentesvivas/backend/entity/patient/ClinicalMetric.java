package com.mentesvivas.backend.entity.patient;

import java.math.BigDecimal;

public enum ClinicalMetric {
    SYSTOLIC, DIASTOLIC, HEART_RATE, FASTING_GLUCOSE, POSTPRANDIAL_GLUCOSE, TEMPERATURE, OXYGEN_SATURATION;

    public BigDecimal valueOf(VitalSign sign) {
        return switch (this) {
            case SYSTOLIC -> decimal(sign.getSystolic());
            case DIASTOLIC -> decimal(sign.getDiastolic());
            case HEART_RATE -> decimal(sign.getHeartRate());
            case FASTING_GLUCOSE -> sign.getGlucoseType() == GlucoseType.FASTING ? decimal(sign.getGlucose()) : null;
            case POSTPRANDIAL_GLUCOSE -> sign.getGlucoseType() == GlucoseType.POSTPRANDIAL
                    ? decimal(sign.getGlucose()) : null;
            case TEMPERATURE -> sign.getTemperature();
            case OXYGEN_SATURATION -> decimal(sign.getOxygenSaturation());
        };
    }

    private static BigDecimal decimal(Integer value) {
        return value == null ? null : BigDecimal.valueOf(value);
    }
}
