package com.mentesvivas.backend.controller.admin;

import com.mentesvivas.backend.dto.audit.ErrataRequest;
import com.mentesvivas.backend.dto.audit.ErrataResponse;
import com.mentesvivas.backend.service.audit.AuditErrataService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/audit-events")
@RequiredArgsConstructor
public class AuditErrataController {

    private final AuditErrataService service;

    @PostMapping("/{id}/errata")
    @ResponseStatus(HttpStatus.CREATED)
    public ErrataResponse correct(@PathVariable UUID id, @Valid @RequestBody ErrataRequest body) {
        return service.correct(id, body.justification());
    }
}
