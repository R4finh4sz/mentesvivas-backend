package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.config.EmailConfirmationProperties;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.auth.InvalidCodeException;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.service.code.CodePolicy;
import com.mentesvivas.backend.service.code.OneTimeCodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmailConfirmationServiceTest {

    private static final String EMAIL = "ana@exemplo.com";

    private final CodePolicy policy = new CodePolicy("email-confirmation", Duration.ofMinutes(30), 5, 3, Duration.ofHours(1));

    @Mock
    private UserRepository userRepository;
    @Mock
    private OneTimeCodeService codeService;
    @Mock
    private EmailConfirmationEmails emails;

    private EmailConfirmationService service;

    @BeforeEach
    void setUp() {
        EmailConfirmationProperties properties = new EmailConfirmationProperties(Duration.ofMinutes(30), 5, 3, Duration.ofHours(1));
        service = new EmailConfirmationService(userRepository, codeService, policy, emails, properties);
    }

    private User unconfirmedUser() {
        User user = new User();
        user.setEmail(EMAIL);
        return user;
    }

    @Test
    void sendCodeSendsTheGeneratedCode() {
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.of("123456"));

        service.sendCode(unconfirmedUser());

        verify(emails).sendCode(EMAIL, "123456");
    }

    @Test
    void sendCodeSkipsConfirmedUsers() {
        User user = unconfirmedUser();
        user.setEmailConfirmed(true);

        service.sendCode(user);

        verifyNoInteractions(codeService, emails);
    }

    @Test
    void sendCodeKeepingFlowSwallowsEmailProviderFailure() {
        when(codeService.generate(policy, EMAIL)).thenReturn(Optional.of("123456"));
        doThrow(new ServiceUnavailableException("down", null)).when(emails).sendCode(EMAIL, "123456");

        assertThatCode(() -> service.sendCodeKeepingFlow(unconfirmedUser())).doesNotThrowAnyException();
    }

    @Test
    void resendForUnknownEmailSendsNothingButAnswersNormally() {
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        var response = service.resend(EMAIL);

        assertThat(response.codeExpiresInSeconds()).isEqualTo(1800);
        verifyNoInteractions(codeService, emails);
    }

    @Test
    void confirmMarksEmailAsConfirmed() {
        User user = unconfirmedUser();
        when(userRepository.findByEmail(EMAIL)).thenReturn(Optional.of(user));

        service.confirm(EMAIL, "123456");

        verify(codeService).validateAndConsume(policy, EMAIL, "123456");
        assertThat(user.isEmailConfirmed()).isTrue();
    }

    @Test
    void confirmWithInvalidCodeLeavesUserUntouched() {
        doThrow(new InvalidCodeException()).when(codeService).validateAndConsume(policy, EMAIL, "000000");

        assertThatThrownBy(() -> service.confirm(EMAIL, "000000")).isInstanceOf(InvalidCodeException.class);

        verifyNoInteractions(userRepository);
    }
}
