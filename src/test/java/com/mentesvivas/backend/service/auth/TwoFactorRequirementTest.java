package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TwoFactorRequirementTest {

    private final TwoFactorRequirement requirement = new TwoFactorRequirement();

    private User userWith(Role role) {
        User user = new User();
        user.setRole(role);
        return user;
    }

    @Test
    void doctorsMustUseTheSecondFactor() {
        assertThat(requirement.isRequiredFor(userWith(Role.DOCTOR))).isTrue();
    }

    @Test
    void otherProfilesDoNot() {
        assertThat(requirement.isRequiredFor(userWith(Role.PATIENT))).isFalse();
        assertThat(requirement.isRequiredFor(userWith(Role.CAREGIVER))).isFalse();
        assertThat(requirement.isRequiredFor(userWith(Role.ADMINISTRATOR))).isFalse();
    }
}
