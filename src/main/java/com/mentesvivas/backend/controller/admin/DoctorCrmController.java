package com.mentesvivas.backend.controller.admin;

import com.mentesvivas.backend.dto.admin.PendingDoctorResponse;
import com.mentesvivas.backend.service.admin.DoctorCrmVerificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/doctors")
@RequiredArgsConstructor
public class DoctorCrmController {

    private final DoctorCrmVerificationService service;

    @GetMapping("/pending-crm")
    public List<PendingDoctorResponse> pending() {
        return service.pending();
    }

    @PostMapping("/{id}/verify-crm")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void verify(@PathVariable UUID id) {
        service.verify(id);
    }
}
