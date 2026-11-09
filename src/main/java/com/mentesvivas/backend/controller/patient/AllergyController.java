package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.AddAllergyRequest;
import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.AllergyStatusResponse;
import com.mentesvivas.backend.dto.patient.SetAllergyStatusRequest;
import com.mentesvivas.backend.service.patient.AllergyService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/me")
@RequiredArgsConstructor
public class AllergyController {

    private final AllergyService allergyService;

    @GetMapping("/allergies")
    public List<AllergyResponse> list() {
        return allergyService.list();
    }

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public AllergyResponse add(@Valid @RequestBody AddAllergyRequest body) {
        return allergyService.add(body);
    }

    @PutMapping("/allergy-status")
    public AllergyStatusResponse setStatus(@Valid @RequestBody SetAllergyStatusRequest body) {
        return new AllergyStatusResponse(allergyService.setStatus(body));
    }
}
