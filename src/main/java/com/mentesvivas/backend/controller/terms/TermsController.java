package com.mentesvivas.backend.controller.terms;

import com.mentesvivas.backend.dto.terms.AcceptTermsRequest;
import com.mentesvivas.backend.dto.terms.PublishTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsAcceptanceResponse;
import com.mentesvivas.backend.dto.terms.TermsResponse;
import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.service.terms.TermsAcceptanceService;
import com.mentesvivas.backend.service.terms.TermsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermsController {

    private final TermsService termsService;
    private final TermsAcceptanceService termsAcceptanceService;

    @GetMapping
    public TermsResponse current(@RequestParam(required = false) TermsType type,
                                 @RequestParam(required = false) Role role) {
        return termsService.current(type, role);
    }

    @GetMapping("/signup")
    public List<TermsResponse> forSignup(@RequestParam Role role) {
        return termsService.currentForSignup(role);
    }

    @GetMapping("/history")
    public List<TermsResponse> history() {
        return termsService.history();
    }

    @GetMapping("/accepted")
    public List<TermsAcceptanceResponse> acceptances() {
        return termsAcceptanceService.currentUserAcceptances();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermsResponse publish(@Valid @RequestBody PublishTermsRequest body) {
        return termsService.publish(body);
    }

    @PostMapping("/accept")
    public TermsAcceptanceResponse accept(@Valid @RequestBody AcceptTermsRequest body) {
        return termsAcceptanceService.accept(body);
    }
}
