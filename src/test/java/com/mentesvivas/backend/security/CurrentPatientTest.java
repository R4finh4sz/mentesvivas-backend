package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentPatientTest {

    @Mock
    private CurrentUser currentUser;

    private User userWith(Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    @Test
    void returnsThePatient() {
        User patient = userWith(Role.PATIENT);
        when(currentUser.get()).thenReturn(patient);

        assertThat(new CurrentPatient(currentUser).get()).isSameAs(patient);
    }

    @Test
    void otherProfilesAreDenied() {
        for (Role role : new Role[]{Role.CAREGIVER, Role.DOCTOR, Role.ADMINISTRATOR}) {
            when(currentUser.get()).thenReturn(userWith(role));

            assertThatThrownBy(() -> new CurrentPatient(currentUser).get()).isInstanceOf(AccessDeniedException.class);
        }
    }
}
