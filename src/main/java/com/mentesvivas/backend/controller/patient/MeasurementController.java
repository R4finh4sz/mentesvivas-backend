package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.MeasurementResponse;
import com.mentesvivas.backend.dto.patient.RecordMeasurementRequest;
import com.mentesvivas.backend.service.patient.MeasurementService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/me/measurements")
@RequiredArgsConstructor
public class MeasurementController {

    private final MeasurementService measurementService;

    @GetMapping
    public List<MeasurementResponse> history() {
        return measurementService.history();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MeasurementResponse record(@Valid @RequestBody RecordMeasurementRequest body) {
        return measurementService.record(body);
    }
}
