package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.dto.patient.EmergencyContactRequest;
import com.mentesvivas.backend.dto.patient.ReplaceEmergencyContactsRequest;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.EmergencyContact;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import com.mentesvivas.backend.security.CurrentPatient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmergencyContactServiceTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock
    private CurrentPatient currentPatient;
    @Mock
    private EmergencyContactRepository repository;
    @Mock
    private SensitiveChangeRecorder changeRecorder;

    private EmergencyContactService service;

    @BeforeEach
    void setUp() {
        service = new EmergencyContactService(currentPatient, repository, changeRecorder);
        org.mockito.Mockito.lenient().when(currentPatient.get()).thenReturn(new User() {
            @Override
            public UUID getId() {
                return PATIENT_ID;
            }
        });
    }

    @Test
    void replaceDeletesTheOldContactsBeforeSavingTheNewOnesSortedByOrder() {
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        var request = new ReplaceEmergencyContactsRequest(List.of(
                new EmergencyContactRequest("Joao", "Filho", "(11) 98888-7777", 2),
                new EmergencyContactRequest("Maria", "Filha", "11977776666", 1)));

        var response = service.replace(request);

        InOrder order = inOrder(repository, changeRecorder);
        order.verify(repository).deleteAllByPatientId(PATIENT_ID);
        order.verify(repository).saveAll(anyList());
        order.verify(changeRecorder).record(eq(PATIENT_ID), eq(SensitiveField.EMERGENCY_CONTACTS),
                eq(ChangeType.REPLACED), anyString(), eq(PATIENT_ID));
        assertThat(response).extracting("name").containsExactly("Maria", "Joao");
        assertThat(response.get(1).phone()).isEqualTo("11988887777");
    }

    @Test
    void validatesThatTwoContactsCannotShareTheSameOrder() {
        var duplicated = new ReplaceEmergencyContactsRequest(List.of(
                new EmergencyContactRequest("Joao", "Filho", "11988887777", 1),
                new EmergencyContactRequest("Maria", "Filha", "11977776666", 1)));

        assertThat(duplicated.isOrderUnique()).isFalse();
    }

    @Test
    void savedEntitiesCarryThePatientId() {
        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        service.replace(new ReplaceEmergencyContactsRequest(List.of(
                new EmergencyContactRequest("Joao", "Filho", "11988887777", 1),
                new EmergencyContactRequest("Maria", "Filha", "11977776666", 2))));

        verify(repository).saveAll(org.mockito.ArgumentMatchers.<List<EmergencyContact>>argThat(
                saved -> saved.stream().allMatch(contact -> PATIENT_ID.equals(contact.getPatientId()))));
    }
}
