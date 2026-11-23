package com.mentesvivas.backend.service.alert;

import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.patient.EmergencyContactRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SosRecipientsTest {

    private static final UUID PATIENT = UUID.randomUUID();

    @Mock
    private CaregiverPatientLinkRepository linkRepository;
    @Mock
    private EmergencyContactRepository contactRepository;

    private SosRecipients recipients;

    @BeforeEach
    void setUp() {
        recipients = new SosRecipients(linkRepository, contactRepository);
    }

    @Test
    void returnsTheActiveCaregivers() {
        UUID caregiver = UUID.randomUUID();
        when(linkRepository.findByPatientIdAndStatus(PATIENT, LinkStatus.ACTIVE))
                .thenReturn(List.of(new CaregiverPatientLink(caregiver, PATIENT, LinkStatus.ACTIVE)));

        assertThat(recipients.requireCaregiversOrContacts(PATIENT)).containsExactly(caregiver);
    }

    @Test
    void emergencyContactsAloneAreEnough() {
        when(linkRepository.findByPatientIdAndStatus(PATIENT, LinkStatus.ACTIVE)).thenReturn(List.of());
        when(contactRepository.countByPatientId(PATIENT)).thenReturn(2L);

        assertThat(recipients.requireCaregiversOrContacts(PATIENT)).isEmpty();
    }

    @Test
    void noCaregiverAndNoContactIsRefused() {
        when(linkRepository.findByPatientIdAndStatus(PATIENT, LinkStatus.ACTIVE)).thenReturn(List.of());
        when(contactRepository.countByPatientId(PATIENT)).thenReturn(0L);

        assertThatThrownBy(() -> recipients.requireCaregiversOrContacts(PATIENT))
                .isInstanceOf(ConflictException.class)
                .hasMessageContaining("Cadastre");
    }
}
