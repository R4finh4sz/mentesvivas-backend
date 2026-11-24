package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.GeofenceReading;
import com.mentesvivas.backend.dto.patient.GeofenceRequest;
import com.mentesvivas.backend.dto.patient.GeofenceResponse;
import com.mentesvivas.backend.entity.patient.Geofence.Position;
import com.mentesvivas.backend.service.patient.GeofenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class GeofenceController {

    private final GeofenceService service;

    @PutMapping("/api/caregivers/me/patients/{patientId}/geofence")
    public GeofenceResponse configure(@PathVariable UUID patientId, @Valid @RequestBody GeofenceRequest body) {
        return service.configure(patientId, body);
    }

    @DeleteMapping("/api/caregivers/me/patients/{patientId}/geofence")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void disable(@PathVariable UUID patientId) {
        service.disable(patientId);
    }

    @GetMapping("/api/patients/me/geofence")
    public GeofenceResponse current() {
        return service.current();
    }

    @PostMapping("/api/patients/me/geofence/readings")
    public ReadingResult read(@Valid @RequestBody GeofenceReading body) {
        return new ReadingResult(service.evaluate(body));
    }

    public record ReadingResult(Position position) {}
}
