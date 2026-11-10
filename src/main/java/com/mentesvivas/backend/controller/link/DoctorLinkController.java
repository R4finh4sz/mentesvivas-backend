package com.mentesvivas.backend.controller.link;

import com.mentesvivas.backend.dto.link.DoctorLinkResponse;
import com.mentesvivas.backend.dto.link.InviteByEmailRequest;
import com.mentesvivas.backend.dto.link.RequestDoctorLinkRequest;
import com.mentesvivas.backend.service.link.DoctorLinkService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/doctor-links")
@RequiredArgsConstructor
public class DoctorLinkController {

    private final DoctorLinkService service;

    @PostMapping("/requests")
    @ResponseStatus(HttpStatus.CREATED)
    public DoctorLinkResponse request(@Valid @RequestBody RequestDoctorLinkRequest body) {
        return service.request(body);
    }

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void invite(@Valid @RequestBody InviteByEmailRequest body) {
        service.invitePatient(body.email());
    }

    @GetMapping
    public List<DoctorLinkResponse> mine() {
        return service.mine();
    }

    @PostMapping("/{id}/accept")
    public DoctorLinkResponse accept(@PathVariable UUID id) {
        return service.accept(id);
    }

    @PostMapping("/{id}/decline")
    public DoctorLinkResponse decline(@PathVariable UUID id) {
        return service.decline(id);
    }

    @PostMapping("/{id}/end")
    public DoctorLinkResponse end(@PathVariable UUID id) {
        return service.end(id);
    }
}
