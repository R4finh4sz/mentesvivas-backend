package com.mentesvivas.backend.controller.link;

import com.mentesvivas.backend.dto.link.CaregiverLinkResponse;
import com.mentesvivas.backend.dto.link.InviteByEmailRequest;
import com.mentesvivas.backend.service.link.CaregiverLinkService;
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
@RequestMapping("/api/caregiver-links")
@RequiredArgsConstructor
public class CaregiverLinkController {

    private final CaregiverLinkService service;

    @PostMapping("/invitations")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public void invite(@Valid @RequestBody InviteByEmailRequest body) {
        service.invite(body.email());
    }

    @GetMapping
    public List<CaregiverLinkResponse> mine() {
        return service.mine();
    }

    @PostMapping("/{id}/accept")
    public CaregiverLinkResponse accept(@PathVariable UUID id) {
        return service.accept(id);
    }

    @PostMapping("/{id}/decline")
    public CaregiverLinkResponse decline(@PathVariable UUID id) {
        return service.decline(id);
    }

    @PostMapping("/{id}/end")
    public CaregiverLinkResponse end(@PathVariable UUID id) {
        return service.end(id);
    }
}
