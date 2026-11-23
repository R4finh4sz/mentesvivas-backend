package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.VitalSignLimitsProperties;
import com.mentesvivas.backend.config.VitalSignLimitsProperties.Range;
import com.mentesvivas.backend.dto.patient.RecordVitalSignRequest;
import com.mentesvivas.backend.entity.patient.GlucoseType;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class VitalSignValidatorTest {

    private static Range range(String min, String max) {
        return new Range(new BigDecimal(min), new BigDecimal(max));
    }

    private final VitalSignValidator validator = new VitalSignValidator(new VitalSignLimitsProperties(
            range("40", "300"), range("20", "200"), range("20", "300"),
            range("10", "1500"), range("25", "45"), range("30", "100")));

    private static RecordVitalSignRequest request(Integer systolic, Integer diastolic, Integer heartRate,
                                                  Integer glucose, String temperature, Integer oxygen) {
        return new RecordVitalSignRequest(systolic, diastolic, heartRate, glucose,
                glucose == null ? null : GlucoseType.FASTING,
                temperature == null ? null : new BigDecimal(temperature), oxygen, null, null);
    }

    @Test
    void valuesInsideTheLimitsAreAccepted() {
        assertThatCode(() -> validator.validate(request(120, 80, 72, 95, "36.5", 98))).doesNotThrowAnyException();
    }

    @Test
    void limitsAreInclusive() {
        assertThatCode(() -> validator.validate(request(300, 200, 20, 10, "25", 30))).doesNotThrowAnyException();
        assertThatCode(() -> validator.validate(request(41, 40, 300, 1500, "45", 100))).doesNotThrowAnyException();
    }

    @Test
    void valuesOutsideTheHumanSpectrumAreBlocked() {
        assertThatThrownBy(() -> validator.validate(request(350, 80, null, null, null, null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Pressao sistolica");
        assertThatThrownBy(() -> validator.validate(request(120, 10, null, null, null, null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Pressao diastolica");
        assertThatThrownBy(() -> validator.validate(request(null, null, 400, null, null, null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Frequencia cardiaca");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, 5, null, null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Glicemia");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, "50.0", null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Temperatura");
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, 101)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("Oxigenacao");
    }

    @Test
    void messageShowsTheAcceptedRange() {
        assertThatThrownBy(() -> validator.validate(request(null, null, null, null, null, 20)))
                .hasMessage("Oxigenacao invalida: aceitamos de 30 a 100 %.");
    }

    @Test
    void systolicMustBeGreaterThanDiastolic() {
        assertThatThrownBy(() -> validator.validate(request(80, 80, null, null, null, null)))
                .isInstanceOf(ImpossibleValueException.class).hasMessageContaining("sistolica deve ser maior");
    }

    @Test
    void absentValuesAreIgnored() {
        assertThatCode(() -> validator.validate(request(null, null, null, null, null, 97))).doesNotThrowAnyException();
    }
}
