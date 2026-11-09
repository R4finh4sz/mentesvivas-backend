package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.config.MeasurementProperties;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import com.mentesvivas.backend.exception.patient.ValueNeedsConfirmationException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MeasurementValidatorTest {

    private final MeasurementValidator validator = new MeasurementValidator(new MeasurementProperties(
            new MeasurementProperties.Limits(bd("30"), bd("300"), bd("10"), bd("500")),
            new MeasurementProperties.Limits(bd("1.20"), bd("2.30"), bd("0.50"), bd("2.80"))));

    private static BigDecimal bd(String value) {
        return new BigDecimal(value);
    }

    @Test
    void valuesInsideTheExpectedRangeAreAccepted() {
        assertThatCode(() -> validator.validateWeight(bd("72.5"), false)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateHeight(bd("1.68"), false)).doesNotThrowAnyException();
    }

    @Test
    void expectedRangeBoundariesAreInclusive() {
        assertThatCode(() -> validator.validateWeight(bd("30"), false)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateWeight(bd("300"), false)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateHeight(bd("1.20"), false)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateHeight(bd("2.30"), false)).doesNotThrowAnyException();
    }

    @Test
    void plausibleButUnexpectedValueNeedsConfirmation() {
        assertThatThrownBy(() -> validator.validateWeight(bd("25"), false))
                .isInstanceOf(ValueNeedsConfirmationException.class)
                .hasMessageContaining("Peso fora do esperado (30 a 300 kg)");
        assertThatThrownBy(() -> validator.validateHeight(bd("1.10"), false))
                .isInstanceOf(ValueNeedsConfirmationException.class)
                .hasMessageContaining("Altura fora do esperado (1.2 a 2.3 m)");
    }

    @Test
    void confirmedUnexpectedValueIsAccepted() {
        assertThatCode(() -> validator.validateWeight(bd("25"), true)).doesNotThrowAnyException();
        assertThatCode(() -> validator.validateHeight(bd("2.50"), true)).doesNotThrowAnyException();
    }

    @Test
    void impossibleValueIsBlockedEvenWhenConfirmed() {
        assertThatThrownBy(() -> validator.validateWeight(bd("5"), true)).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> validator.validateWeight(bd("501"), true)).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> validator.validateHeight(bd("0.30"), true)).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> validator.validateHeight(bd("3.00"), true)).isInstanceOf(ImpossibleValueException.class);
    }
}
