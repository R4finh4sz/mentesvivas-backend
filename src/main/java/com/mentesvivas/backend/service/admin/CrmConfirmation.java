package com.mentesvivas.backend.service.admin;

import com.mentesvivas.backend.entity.user.User;

public interface CrmConfirmation {

    boolean confirms(User doctor);
}
