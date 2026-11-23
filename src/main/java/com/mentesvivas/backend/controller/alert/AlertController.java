package com.mentesvivas.backend.controller.alert;

import com.mentesvivas.backend.dto.alert.AlertDetailResponse;
import com.mentesvivas.backend.dto.alert.AlertResponse;
import com.mentesvivas.backend.service.alert.AlertService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertService service;

    @GetMapping
    public List<AlertResponse> list(@PathVariable UUID patientId,
                                    @RequestParam(defaultValue = "false") boolean pending) {
        return service.list(patientId, pending);
    }

    @GetMapping("/{alertId}")
    public AlertDetailResponse detail(@PathVariable UUID patientId, @PathVariable UUID alertId) {
        return service.detail(patientId, alertId);
    }

    @PostMapping("/{alertId}/seen")
    public AlertResponse markSeen(@PathVariable UUID patientId, @PathVariable UUID alertId) {
        return service.markSeen(patientId, alertId);
    }

    @PostMapping("/{alertId}/acknowledge")
    public AlertResponse acknowledge(@PathVariable UUID patientId, @PathVariable UUID alertId) {
        return service.acknowledge(patientId, alertId);
    }

    @PostMapping("/{alertId}/resolve")
    public AlertResponse resolve(@PathVariable UUID patientId, @PathVariable UUID alertId) {
        return service.resolve(patientId, alertId);
    }
}
