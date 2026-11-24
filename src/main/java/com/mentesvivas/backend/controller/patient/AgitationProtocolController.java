package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.AgitationProtocolResponse;
import com.mentesvivas.backend.entity.patient.OccurrenceSeverity;
import com.mentesvivas.backend.service.patient.AgitationProtocolService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class AgitationProtocolController {

    private final AgitationProtocolService protocols;

    @PostMapping("/api/doctors/me/agitation-protocols/{severity}")
    @ResponseStatus(HttpStatus.CREATED)
    public AgitationProtocolResponse publish(@PathVariable OccurrenceSeverity severity,
                                              @Valid @RequestBody PublishRequest request) {
        return protocols.publish(severity, request.instructions());
    }

    @GetMapping("/api/caregivers/me/patients/{patientId}/crisis")
    public AgitationProtocolService.CrisisDetails crisis(@PathVariable UUID patientId,
                                                          @RequestParam OccurrenceSeverity severity) {
        return protocols.crisis(patientId, severity);
    }

    public record PublishRequest(@NotBlank @Size(max = 4000) String instructions) {}
}
