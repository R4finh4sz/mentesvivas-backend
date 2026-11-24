package com.mentesvivas.backend.controller.game;

import com.mentesvivas.backend.dto.game.FamilyGalleryResponse;
import com.mentesvivas.backend.dto.game.FamilyMemberResponse;
import com.mentesvivas.backend.dto.game.RegisterFamilyMemberRequest;
import com.mentesvivas.backend.service.game.FamilyMemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/family-members")
@RequiredArgsConstructor
public class FamilyMemberController {

    private final FamilyMemberService service;

    @GetMapping
    public FamilyGalleryResponse gallery(@PathVariable UUID patientId) {
        return service.gallery(patientId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FamilyMemberResponse register(@PathVariable UUID patientId,
                                         @Valid @RequestBody RegisterFamilyMemberRequest body) {
        return service.register(patientId, body);
    }

    @DeleteMapping("/{memberId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(@PathVariable UUID patientId, @PathVariable UUID memberId) {
        service.remove(patientId, memberId);
    }
}
