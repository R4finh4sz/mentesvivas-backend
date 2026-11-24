package com.mentesvivas.backend.controller.clinical;

import com.mentesvivas.backend.service.clinical.DiagnosisService;
import com.mentesvivas.backend.service.clinical.DiagnosisService.CatalogInput;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class Cid10Controller {

    private final DiagnosisService diagnosis;

    @PostMapping("/api/admin/cid10/catalog")
    public ImportResult load(@Valid @RequestBody ImportRequest request) {
        return new ImportResult(diagnosis.importCatalog(request.entries().stream()
                .map(entry -> new CatalogInput(entry.code(), entry.description())).toList()));
    }

    @GetMapping("/api/doctors/me/cid10")
    public List<CatalogInput> search(@RequestParam @Size(max = 5) String code) {
        return diagnosis.search(code);
    }

    public record ImportRequest(@NotNull @Size(min = 1, max = 5000) List<@NotNull @Valid Entry> entries) {}

    public record Entry(@NotBlank @Size(max = 5) String code, @NotBlank @Size(max = 400) String description) {}

    public record ImportResult(int imported) {}
}
