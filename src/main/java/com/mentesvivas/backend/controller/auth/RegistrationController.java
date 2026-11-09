package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.user.DoctorRegistrationRequest;
import com.mentesvivas.backend.dto.user.RegistrationRequest;
import com.mentesvivas.backend.dto.user.UserResponse;
import com.mentesvivas.backend.service.user.RegistrationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class RegistrationController {

    private final RegistrationService registrationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegistrationRequest body) {
        return registrationService.register(body);
    }

    @PostMapping("/doctor")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse registerDoctor(@Valid @RequestBody DoctorRegistrationRequest body) {
        return registrationService.registerDoctor(body);
    }
}
