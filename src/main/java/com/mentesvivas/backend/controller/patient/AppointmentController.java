package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.AppointmentResponse;
import com.mentesvivas.backend.dto.patient.SaveAppointmentRequest;
import com.mentesvivas.backend.service.patient.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/appointments")
@RequiredArgsConstructor
public class AppointmentController {

    private final AppointmentService service;

    @GetMapping
    public List<AppointmentResponse> list(
            @PathVariable UUID patientId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return service.list(patientId, from, to);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentResponse create(@PathVariable UUID patientId, @Valid @RequestBody SaveAppointmentRequest body) {
        return service.create(patientId, body);
    }

    @PutMapping("/{appointmentId}")
    public AppointmentResponse update(@PathVariable UUID patientId, @PathVariable UUID appointmentId,
                                      @Valid @RequestBody SaveAppointmentRequest body) {
        return service.update(patientId, appointmentId, body);
    }

    @DeleteMapping("/{appointmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID patientId, @PathVariable UUID appointmentId) {
        service.delete(patientId, appointmentId);
    }
}
