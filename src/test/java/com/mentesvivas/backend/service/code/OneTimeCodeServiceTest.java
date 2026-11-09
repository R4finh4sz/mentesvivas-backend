package com.mentesvivas.backend.service.code;

import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OneTimeCodeServiceTest {

    private static final String EMAIL = "ana@exemplo.com";
    private static final CodePolicy POLICY = new CodePolicy("recovery", Duration.ofMinutes(30), 3, 2, Duration.ofHours(1));
    private static final CodePolicy OTHER_POLICY = new CodePolicy("email-confirmation", Duration.ofMinutes(30), 3, 2, Duration.ofHours(1));

    private InMemoryTemporaryStorage memory;
    private OneTimeCodeService service;

    @BeforeEach
    void setUp() {
        memory = new InMemoryTemporaryStorage();
        service = new OneTimeCodeService(memory, memory, new TokenHasher());
    }

    @Test
    void generatesSixDigitCode() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();

        assertThat(code).matches("\\d{6}");
    }

    @Test
    void doesNotStoreCodeInPlainText() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();

        assertThat(memory.allValues()).noneMatch(value -> value.contains(code));
    }

    @Test
    void correctCodeIsAcceptedOnlyOnce() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();

        assertThatCode(() -> service.validateAndConsume(POLICY, EMAIL, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateAndConsume(POLICY, EMAIL, code))
                .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void codeFromAnotherEmailIsRejected() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();

        assertThatThrownBy(() -> service.validateAndConsume(POLICY, "outra@exemplo.com", code))
                .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void withoutGeneratedCodeIsRejected() {
        assertThatThrownBy(() -> service.validateAndConsume(POLICY, EMAIL, "123456"))
                .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void exceedingAttemptsInvalidatesCorrectCode() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();
        String wrong = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.validateAndConsume(POLICY, EMAIL, wrong))
                    .isInstanceOf(InvalidCodeException.class);
        }

        assertThatThrownBy(() -> service.validateAndConsume(POLICY, EMAIL, code))
                .isInstanceOf(InvalidCodeException.class);
    }

    @Test
    void limitsSendsWithinWindow() {
        assertThat(service.generate(POLICY, EMAIL)).isPresent();
        assertThat(service.generate(POLICY, EMAIL)).isPresent();
        assertThat(service.generate(POLICY, EMAIL)).isEmpty();
    }

    @Test
    void newCodeReplacesPrevious() {
        String first = service.generate(POLICY, EMAIL).orElseThrow();
        String second = service.generate(POLICY, EMAIL).orElseThrow();

        if (!first.equals(second)) {
            assertThatThrownBy(() -> service.validateAndConsume(POLICY, EMAIL, first))
                    .isInstanceOf(InvalidCodeException.class);
        }
        assertThatCode(() -> service.validateAndConsume(POLICY, EMAIL, second)).doesNotThrowAnyException();
    }

    @Test
    void codeOfOnePolicyIsNotValidForAnother() {
        String code = service.generate(POLICY, EMAIL).orElseThrow();

        assertThatThrownBy(() -> service.validateAndConsume(OTHER_POLICY, EMAIL, code))
                .isInstanceOf(InvalidCodeException.class);
        assertThatCode(() -> service.validateAndConsume(POLICY, EMAIL, code)).doesNotThrowAnyException();
    }
}
