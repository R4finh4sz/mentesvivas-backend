package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.user.User;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ManualCrmConfirmationTest {

    @Test
    void theAdministratorActionIsTheConfirmation() {
        CrmConfirmation confirmation = new ManualCrmConfirmation();

        assertThat(confirmation.confirms(new User())).isTrue();
    }
}
