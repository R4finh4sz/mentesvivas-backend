package com.mentesvivas.backend.service.user;

import com.mentesvivas.backend.dto.user.UpdateProfileRequest;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.user.EmailAlreadyRegisteredException;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.auth.EmailConfirmationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private CurrentUser currentUser;
    @Mock
    private UserUniquenessValidator uniquenessValidator;
    @Mock
    private EmailConfirmationService emailConfirmationService;

    private ProfileService service;
    private User user;

    @BeforeEach
    void setUp() {
        service = new ProfileService(currentUser, uniquenessValidator, emailConfirmationService);
        user = new User();
        user.setFullName("Mara");
        user.setEmail("mara@exemplo.com");
        user.setPhone("11999998888");
        user.setBirthDate(LocalDate.of(1950, 3, 10));
        user.setRole(Role.PATIENT);
        user.setEmailConfirmed(true);
        when(currentUser.get()).thenReturn(user);
    }

    @Test
    void nullFieldsAreLeftUntouched() {
        service.update(new UpdateProfileRequest(null, null, null, null));

        assertThat(user.getFullName()).isEqualTo("Mara");
        assertThat(user.getPhone()).isEqualTo("11999998888");
        assertThat(user.getBirthDate()).isEqualTo(LocalDate.of(1950, 3, 10));
        assertThat(user.isEmailConfirmed()).isTrue();
    }

    @Test
    void photoLinkIsSavedReplacedAndRemovedWithAnEmptyValue() {
        service.update(new UpdateProfileRequest(null, null, null, null, "https://fotos.exemplo.com/mara.jpg"));
        assertThat(user.getPhotoUrl()).isEqualTo("https://fotos.exemplo.com/mara.jpg");

        service.update(new UpdateProfileRequest(null, null, null, null, null));
        assertThat(user.getPhotoUrl()).isEqualTo("https://fotos.exemplo.com/mara.jpg");

        var response = service.update(new UpdateProfileRequest(null, null, null, null, ""));
        assertThat(user.getPhotoUrl()).isNull();
        assertThat(response.photoUrl()).isNull();
    }

    @Test
    void informedFieldsAreApplied() {
        var response = service.update(new UpdateProfileRequest("Mara Souza", LocalDate.of(1951, 1, 2), "11977776666", null));

        assertThat(response.fullName()).isEqualTo("Mara Souza");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1951, 1, 2));
        assertThat(response.phone()).isEqualTo("11977776666");
    }

    @Test
    void newEmailResetsConfirmationAndSendsCode() {
        service.update(new UpdateProfileRequest(null, null, null, "nova@exemplo.com"));

        assertThat(user.getEmail()).isEqualTo("nova@exemplo.com");
        assertThat(user.isEmailConfirmed()).isFalse();
        verify(uniquenessValidator).validateEmail("nova@exemplo.com");
        verify(emailConfirmationService).sendCodeKeepingFlow(user);
    }

    @Test
    void sameEmailDoesNotTriggerValidationOrNewCode() {
        service.update(new UpdateProfileRequest(null, null, null, "mara@exemplo.com"));

        assertThat(user.isEmailConfirmed()).isTrue();
        verify(uniquenessValidator, never()).validateEmail("mara@exemplo.com");
        verify(emailConfirmationService, never()).sendCodeKeepingFlow(user);
    }

    @Test
    void emailInUseKeepsTheCurrentOne() {
        doThrow(new EmailAlreadyRegisteredException()).when(uniquenessValidator).validateEmail("outra@exemplo.com");

        assertThatThrownBy(() -> service.update(new UpdateProfileRequest(null, null, null, "outra@exemplo.com")))
                .isInstanceOf(EmailAlreadyRegisteredException.class);

        assertThat(user.getEmail()).isEqualTo("mara@exemplo.com");
        assertThat(user.isEmailConfirmed()).isTrue();
    }
}
