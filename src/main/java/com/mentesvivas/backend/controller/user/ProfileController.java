package com.mentesvivas.backend.controller.user;

import com.mentesvivas.backend.dto.user.ProfileResponse;
import com.mentesvivas.backend.dto.user.UpdateProfileRequest;
import com.mentesvivas.backend.service.user.ProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public ProfileResponse get() {
        return profileService.get();
    }

    @PatchMapping
    public ProfileResponse update(@Valid @RequestBody UpdateProfileRequest body) {
        return profileService.update(body);
    }
}
