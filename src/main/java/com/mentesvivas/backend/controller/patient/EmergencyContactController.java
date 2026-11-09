package com.mentesvivas.backend.controller.patient;

import com.mentesvivas.backend.dto.patient.EmergencyContactResponse;
import com.mentesvivas.backend.dto.patient.ReplaceEmergencyContactsRequest;
import com.mentesvivas.backend.service.patient.EmergencyContactService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/patients/me/emergency-contacts")
@RequiredArgsConstructor
public class EmergencyContactController {

    private final EmergencyContactService emergencyContactService;

    @GetMapping
    public List<EmergencyContactResponse> list() {
        return emergencyContactService.list();
    }

    @PutMapping
    public List<EmergencyContactResponse> replace(@Valid @RequestBody ReplaceEmergencyContactsRequest body) {
        return emergencyContactService.replace(body);
    }
}
