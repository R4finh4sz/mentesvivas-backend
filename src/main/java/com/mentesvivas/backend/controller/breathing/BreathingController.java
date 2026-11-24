package com.mentesvivas.backend.controller.breathing;

import com.mentesvivas.backend.dto.breathing.BreathingExerciseResponse;
import com.mentesvivas.backend.dto.breathing.CreateBreathingExerciseRequest;
import com.mentesvivas.backend.service.breathing.BreathingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class BreathingController {

    private final BreathingService service;

    @PostMapping("/api/doctors/me/breathing-exercises")
    @ResponseStatus(HttpStatus.CREATED)
    public BreathingExerciseResponse.Validated validate(@Valid @RequestBody CreateBreathingExerciseRequest body) {
        return service.validate(body);
    }

    @GetMapping("/api/admin/breathing-exercises")
    public List<BreathingExerciseResponse.Validated> all() {
        return service.all();
    }

    @PutMapping("/api/admin/breathing-exercises/{id}/publication")
    public BreathingExerciseResponse.Validated publication(@PathVariable UUID id,
                                                           @Valid @RequestBody PublicationRequest body) {
        return service.changePublication(id, body.published());
    }

    @GetMapping("/api/patients/me/breathing-exercises")
    public BreathingExerciseResponse.Catalog catalog() {
        return service.catalog();
    }

    @PostMapping("/api/patients/me/breathing-exercises/{id}/start")
    public BreathingExerciseResponse.Start start(@PathVariable UUID id) {
        return service.start(id);
    }

    public record PublicationRequest(@NotNull Boolean published) {}
}
