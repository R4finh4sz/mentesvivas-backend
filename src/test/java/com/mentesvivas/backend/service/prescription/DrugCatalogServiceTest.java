package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.dto.prescription.SaveDrugRequest;
import com.mentesvivas.backend.entity.audit.AuditEventType;
import com.mentesvivas.backend.entity.audit.AuditRecord;
import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.prescription.DrugCatalogRepository;
import com.mentesvivas.backend.security.CurrentUser;
import com.mentesvivas.backend.service.audit.AuditTrail;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DrugCatalogServiceTest {

    @Mock
    private DrugCatalogRepository repository;
    @Mock
    private CurrentUser currentUser;
    @Mock
    private AuditTrail auditTrail;

    private DrugCatalogService service;
    private User admin;

    @BeforeEach
    void setUp() {
        service = new DrugCatalogService(repository, currentUser, auditTrail,
                Clock.fixed(Instant.parse("2026-11-23T12:00:00Z"), ZoneOffset.UTC));
        admin = new User();
        admin.setId(UUID.randomUUID());
        lenient().when(currentUser.get()).thenReturn(admin);
        lenient().when(repository.save(any(DrugCatalogEntry.class))).thenAnswer(call -> call.getArgument(0));
    }

    private SaveDrugRequest request(String name, boolean controlled) {
        return new SaveDrugRequest(name, "Dipirona sodica", "Analgesico", controlled, null);
    }

    @Test
    void createStoresTheNormalizedSearchKeysAndAuditsTheChange() {
        var response = service.create(request("  Novalgina ", false));

        ArgumentCaptor<DrugCatalogEntry> saved = ArgumentCaptor.forClass(DrugCatalogEntry.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getSearchKey()).isEqualTo("novalgina");
        assertThat(saved.getValue().getIngredientKey()).isEqualTo("dipirona sodica");
        assertThat(saved.getValue().isActive()).isTrue();
        assertThat(response.name()).isEqualTo("Novalgina");
        ArgumentCaptor<AuditRecord> record = ArgumentCaptor.forClass(AuditRecord.class);
        verify(auditTrail).record(record.capture());
        assertThat(record.getValue().type()).isEqualTo(AuditEventType.DRUG_CATALOG_CHANGED);
        assertThat(record.getValue().actorUserId()).isEqualTo(admin.getId());
    }

    @Test
    void duplicateNameIsAConflictIgnoringAccentsAndCase() {
        when(repository.existsBySearchKey("dipirona")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("DIPIRONA", false))).isInstanceOf(ConflictException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void updateChangesTheControlledFlag() {
        UUID id = UUID.randomUUID();
        DrugCatalogEntry existing = new DrugCatalogEntry();
        existing.setName("Rivotril");
        when(repository.findById(id)).thenReturn(Optional.of(existing));

        var response = service.update(id, request("Rivotril", true));

        assertThat(response.controlled()).isTrue();
    }

    @Test
    void updateOfUnknownDrugIsNotFound() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(id, request("X", false))).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateCannotTakeTheNameOfAnotherDrug() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.of(new DrugCatalogEntry()));
        when(repository.existsBySearchKeyAndIdNot("dipirona", id)).thenReturn(true);

        assertThatThrownBy(() -> service.update(id, request("Dipirona", false))).isInstanceOf(ConflictException.class);
    }
}
