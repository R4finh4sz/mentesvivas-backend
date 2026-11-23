package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.DrugResponse;
import com.mentesvivas.backend.dto.prescription.SaveDrugRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.DrugCatalogRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import com.mentesvivas.backend.util.PlainText;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DrugCatalogService {

    private final DrugCatalogRepository repository;
    private final CurrentUser currentUser;
    private final AuditTrail auditTrail;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<DrugResponse> list() {
        return repository.findAllByOrderByNameAsc().stream().map(DrugCatalogService::toResponse).toList();
    }

    @Transactional
    public DrugResponse create(SaveDrugRequest request) {
        String key = PlainText.key(request.name());
        if (repository.existsBySearchKey(key)) {
            throw new ConflictException("Este medicamento ja esta na base.");
        }
        DrugCatalogEntry entry = new DrugCatalogEntry();
        entry.setCreatedAt(clock.instant());
        fill(entry, request, key);
        return save(entry);
    }

    @Transactional
    public DrugResponse update(UUID id, SaveDrugRequest request) {
        DrugCatalogEntry entry = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Medicamento da base nao encontrado."));
        String key = PlainText.key(request.name());
        if (repository.existsBySearchKeyAndIdNot(key, id)) {
            throw new ConflictException("Ja existe outro medicamento com este nome na base.");
        }
        fill(entry, request, key);
        return save(entry);
    }

    private void fill(DrugCatalogEntry entry, SaveDrugRequest request, String key) {
        entry.setName(request.name());
        entry.setSearchKey(key);
        entry.setActiveIngredient(request.activeIngredient());
        entry.setIngredientKey(PlainText.key(request.activeIngredient()));
        entry.setTherapeuticClass(request.therapeuticClass());
        entry.setControlled(request.controlled());
        entry.setActive(request.isActive());
    }

    private DrugResponse save(DrugCatalogEntry entry) {
        DrugCatalogEntry saved = repository.save(entry);
        auditTrail.record(AuditRecord.of(AuditEventType.DRUG_CATALOG_CHANGED, currentUser.get().getId())
                .withValues(null, saved.getName()));
        return toResponse(saved);
    }

    private static DrugResponse toResponse(DrugCatalogEntry entry) {
        return new DrugResponse(entry.getId(), entry.getName(), entry.getActiveIngredient(),
                entry.getTherapeuticClass(), entry.isControlled(), entry.isActive());
    }
}
