package com.mentesvivas.backend.service.patient.routine;

import com.mentesvivas.backend.dto.patient.RecordRoutineRequest;
import com.mentesvivas.backend.entity.patient.BathCooperation;
import com.mentesvivas.backend.entity.patient.MealPeriod;
import com.mentesvivas.backend.entity.patient.Mood;
import com.mentesvivas.backend.entity.patient.RoutineType;
import com.mentesvivas.backend.entity.patient.SleepQuality;
import com.mentesvivas.backend.entity.patient.UrineAspect;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RoutineRulesTest {

    private RecordRoutineRequest request(RoutineType type, Mood mood, SleepQuality quality, String hours,
                                         MealPeriod period, Boolean accepted, Integer ml, BathCooperation bath,
                                         Integer urineFrequency, UrineAspect aspect, Integer stoolFrequency,
                                         Integer bristol) {
        return new RecordRoutineRequest(type, mood, quality, hours == null ? null : new BigDecimal(hours), period,
                accepted, ml, bath, urineFrequency, aspect, stoolFrequency, bristol, null, null, null);
    }

    private RecordRoutineRequest empty(RoutineType type) {
        return request(type, null, null, null, null, null, null, null, null, null, null, null);
    }

    @Test
    void moodRequiresTheMood() {
        MoodRule rule = new MoodRule();

        assertThatThrownBy(() -> rule.validate(empty(RoutineType.MOOD))).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.MOOD, Mood.CALM, null, null, null, null, null, null,
                null, null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void sleepRequiresQualityAndHoursBetweenZeroAndTwentyFour() {
        SleepRule rule = new SleepRule();
        RecordRoutineRequest noQuality = request(RoutineType.SLEEP, null, null, "7", null, null, null, null, null,
                null, null, null);
        RecordRoutineRequest tooLong = request(RoutineType.SLEEP, null, SleepQuality.GOOD, "25", null, null, null,
                null, null, null, null, null);
        RecordRoutineRequest noHours = request(RoutineType.SLEEP, null, SleepQuality.GOOD, null, null, null, null,
                null, null, null, null, null);

        assertThatThrownBy(() -> rule.validate(noQuality)).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> rule.validate(tooLong)).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> rule.validate(noHours)).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.SLEEP, null, SleepQuality.POOR, "6.5", null, null,
                null, null, null, null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void mealRequiresPeriodAndWhetherItWasAccepted() {
        MealRule rule = new MealRule();

        assertThatThrownBy(() -> rule.validate(request(RoutineType.MEAL, null, null, null, MealPeriod.LUNCH, null,
                null, null, null, null, null, null))).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> rule.validate(request(RoutineType.MEAL, null, null, null, null, true, null, null,
                null, null, null, null))).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.MEAL, null, null, null, MealPeriod.LUNCH, false,
                null, null, null, null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void hydrationAcceptsOnlyAPlausibleAmount() {
        HydrationRule rule = new HydrationRule();

        for (Integer ml : new Integer[]{null, 0, -5, 5001}) {
            assertThatThrownBy(() -> rule.validate(request(RoutineType.HYDRATION, null, null, null, null, null, ml,
                    null, null, null, null, null))).isInstanceOf(ImpossibleValueException.class);
        }
        assertThatCode(() -> rule.validate(request(RoutineType.HYDRATION, null, null, null, null, null, 200, null,
                null, null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void bathRequiresTheCooperation() {
        BathRule rule = new BathRule();

        assertThatThrownBy(() -> rule.validate(empty(RoutineType.BATH))).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.BATH, null, null, null, null, null, null,
                BathCooperation.REFUSED, null, null, null, null))).doesNotThrowAnyException();
    }

    @Test
    void urineRequiresFrequencyAndAspect() {
        UrineRule rule = new UrineRule();

        assertThatThrownBy(() -> rule.validate(request(RoutineType.URINE, null, null, null, null, null, null, null,
                3, null, null, null))).isInstanceOf(ImpossibleValueException.class);
        assertThatThrownBy(() -> rule.validate(request(RoutineType.URINE, null, null, null, null, null, null, null,
                31, UrineAspect.CLEAR, null, null))).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.URINE, null, null, null, null, null, null, null,
                3, UrineAspect.DARK, null, null))).doesNotThrowAnyException();
    }

    @Test
    void stoolRequiresFrequencyAndBristolFromOneToSeven() {
        StoolRule rule = new StoolRule();

        for (Integer bristol : new Integer[]{null, 0, 8}) {
            assertThatThrownBy(() -> rule.validate(request(RoutineType.STOOL, null, null, null, null, null, null,
                    null, null, null, 1, bristol))).isInstanceOf(ImpossibleValueException.class);
        }
        assertThatThrownBy(() -> rule.validate(request(RoutineType.STOOL, null, null, null, null, null, null, null,
                null, null, null, 4))).isInstanceOf(ImpossibleValueException.class);
        assertThatCode(() -> rule.validate(request(RoutineType.STOOL, null, null, null, null, null, null, null,
                null, null, 2, 4))).doesNotThrowAnyException();
    }
}
