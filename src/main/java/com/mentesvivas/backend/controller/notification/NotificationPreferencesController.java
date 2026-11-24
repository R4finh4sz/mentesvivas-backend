package com.mentesvivas.backend.controller.notification;

import com.mentesvivas.backend.dto.notification.NotificationPreferences;
import com.mentesvivas.backend.service.notification.NotificationPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/caregivers/me/notification-preferences")
@RequiredArgsConstructor
public class NotificationPreferencesController {

    private final NotificationPreferenceService preferences;

    @GetMapping
    public NotificationPreferences mine() {
        return preferences.mine();
    }

    @PutMapping
    public NotificationPreferences save(@Valid @RequestBody NotificationPreferences request) {
        return preferences.save(request);
    }
}
