package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.PasswordRecoveryProperties;
import com.mentesvivas.backend.exception.auth.InvalidRecoveryCodeException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import com.mentesvivas.backend.support.InMemoryTemporaryStorage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RecoveryCodeServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private InMemoryTemporaryStorage memory;
    private RecoveryCodeService service;

    @BeforeEach
    void setUp() {
        memory = new InMemoryTemporaryStorage();
        PasswordRecoveryProperties properties = new PasswordRecoveryProperties(Duration.ofMinutes(30), 3, 2, Duration.ofHours(1));
        service = new RecoveryCodeService(memory, memory, properties, new TokenHasher());
    }

    @Test
    void generatesSixDigitCode() {
        String code = service.generate(EMAIL).orElseThrow();

        assertThat(code).matches("\\d{6}");
    }

    @Test
    void doesNotStoreCodeInPlainText() {
        String code = service.generate(EMAIL).orElseThrow();

        assertThat(memory.allValues()).noneMatch(value -> value.contains(code));
    }

    @Test
    void correctCodeIsAcceptedOnlyOnce() {
        String code = service.generate(EMAIL).orElseThrow();

        assertThatCode(() -> service.validateAndConsume(EMAIL, code)).doesNotThrowAnyException();
        assertThatThrownBy(() -> service.validateAndConsume(EMAIL, code))
                .isInstanceOf(InvalidRecoveryCodeException.class);
    }

    @Test
    void codeFromAnotherEmailIsRejected() {
        String code = service.generate(EMAIL).orElseThrow();

        assertThatThrownBy(() -> service.validateAndConsume("outra@exemplo.com", code))
                .isInstanceOf(InvalidRecoveryCodeException.class);
    }

    @Test
    void withoutGeneratedCodeIsRejected() {
        assertThatThrownBy(() -> service.validateAndConsume(EMAIL, "123456"))
                .isInstanceOf(InvalidRecoveryCodeException.class);
    }

    @Test
    void exceedingAttemptsInvalidatesCorrectCode() {
        String code = service.generate(EMAIL).orElseThrow();
        String wrong = code.equals("000000") ? "000001" : "000000";
        for (int i = 0; i < 3; i++) {
            assertThatThrownBy(() -> service.validateAndConsume(EMAIL, wrong))
                    .isInstanceOf(InvalidRecoveryCodeException.class);
        }

        assertThatThrownBy(() -> service.validateAndConsume(EMAIL, code))
                .isInstanceOf(InvalidRecoveryCodeException.class);
    }

    @Test
    void limitsSendsWithinWindow() {
        assertThat(service.generate(EMAIL)).isPresent();
        assertThat(service.generate(EMAIL)).isPresent();
        assertThat(service.generate(EMAIL)).isEmpty();
    }

    @Test
    void newCodeReplacesPrevious() {
        String first = service.generate(EMAIL).orElseThrow();
        String second = service.generate(EMAIL).orElseThrow();

        if (!first.equals(second)) {
            assertThatThrownBy(() -> service.validateAndConsume(EMAIL, first))
                    .isInstanceOf(InvalidRecoveryCodeException.class);
        }
        assertThatCode(() -> service.validateAndConsume(EMAIL, second)).doesNotThrowAnyException();
    }
}
