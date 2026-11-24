package com.mentesvivas.backend.controller.prescription;

import com.mentesvivas.backend.dto.prescription.DrugResponse;
import com.mentesvivas.backend.dto.prescription.SaveDrugRequest;
import com.mentesvivas.backend.service.prescription.DrugCatalogService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/drugs")
@RequiredArgsConstructor
public class DrugCatalogController {

    private final DrugCatalogService service;

    @GetMapping
    public List<DrugResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public DrugResponse create(@Valid @RequestBody SaveDrugRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public DrugResponse update(@PathVariable UUID id, @Valid @RequestBody SaveDrugRequest body) {
        return service.update(id, body);
    }
}
