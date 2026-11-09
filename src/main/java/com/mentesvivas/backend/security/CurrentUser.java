package com.mentesvivas.backend.security;

import com.mentesvivas.backend.entity.user.User;

public interface CurrentUser {

    User get();
}
