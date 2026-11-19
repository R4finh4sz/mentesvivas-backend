package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.user.User;
import org.springframework.stereotype.Component;

@Component
public class ManualCrmConfirmation implements CrmConfirmation {

    @Override
    public boolean confirms(User doctor) {
        return true;
    }
}
