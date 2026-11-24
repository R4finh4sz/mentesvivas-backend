package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CaregiverMomentControllerTest {

    private final CurrentUser currentUser = mock(CurrentUser.class);
    private final CaregiverMomentController controller = new CaregiverMomentController(currentUser);

    @Test
    void caregiverCanReadContentAndFindExhaustionSupport() {
        authenticate(Role.CAREGIVER);

        assertThat(controller.content().relaxation()).isNotEmpty();
        assertThat(controller.content().ergonomics()).isNotEmpty();
        assertThat(controller.exhaustion().phone()).isEqualTo("188");
    }

    @Test
    void patientCannotAccessTheCaregiverModule() {
        authenticate(Role.PATIENT);

        assertThatThrownBy(controller::content).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(controller::exhaustion).isInstanceOf(AccessDeniedException.class);
    }

    private void authenticate(Role role) {
        User user = new User();
        user.setRole(role);
        when(currentUser.get()).thenReturn(user);
    }
}
