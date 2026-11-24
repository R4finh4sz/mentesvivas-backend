package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import com.mentesvivas.backend.repository.prescription.DrugCatalogRepository;
import com.mentesvivas.backend.util.PlainText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class DrugLookup {

    private final DrugCatalogRepository repository;

    public Optional<DrugCatalogEntry> find(String medicationName) {
        String key = PlainText.key(medicationName);
        if (key.isEmpty()) {
            return Optional.empty();
        }
        return repository.findFirstByActiveTrueAndSearchKeyOrActiveTrueAndIngredientKey(key, key);
    }
}
