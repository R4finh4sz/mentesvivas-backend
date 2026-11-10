package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.dto.clinical.DeclareDiseaseRequest;
import com.mentesvivas.backend.dto.patient.AddAllergyRequest;
import com.mentesvivas.backend.dto.patient.AllergyResponse;
import com.mentesvivas.backend.dto.patient.AllergyStatusResponse;
import com.mentesvivas.backend.dto.patient.SetAllergyStatusRequest;
import com.mentesvivas.backend.service.clinical.DoctorClinicalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/doctors/me/patients/{patientId}")
@RequiredArgsConstructor
public class DoctorClinicalController {

    private final DoctorClinicalService service;

    @PostMapping("/allergies")
    @ResponseStatus(HttpStatus.CREATED)
    public AllergyResponse addAllergy(@PathVariable UUID patientId, @Valid @RequestBody AddAllergyRequest body) {
        return service.addAllergy(patientId, body);
    }

    @PutMapping("/allergies/{allergyId}")
    public AllergyResponse updateAllergy(@PathVariable UUID patientId, @PathVariable UUID allergyId,
                                         @Valid @RequestBody AddAllergyRequest body) {
        return service.updateAllergy(patientId, allergyId, body);
    }

    @DeleteMapping("/allergies/{allergyId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeAllergy(@PathVariable UUID patientId, @PathVariable UUID allergyId) {
        service.removeAllergy(patientId, allergyId);
    }

    @PutMapping("/allergy-status")
    public AllergyStatusResponse setAllergyStatus(@PathVariable UUID patientId,
                                                  @Valid @RequestBody SetAllergyStatusRequest body) {
        return new AllergyStatusResponse(service.setAllergyStatus(patientId, body));
    }

    @PutMapping("/declared-disease")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void validateDisease(@PathVariable UUID patientId, @Valid @RequestBody DeclareDiseaseRequest body) {
        service.validateDisease(patientId, body);
    }
}
