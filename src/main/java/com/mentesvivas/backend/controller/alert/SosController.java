package com.mentesvivas.backend.controller.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.service.alert.SosService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/patients/me/sos")
@RequiredArgsConstructor
public class SosController {

    private final SosService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AlertDetailResponse trigger() {
        return service.trigger();
    }
}
