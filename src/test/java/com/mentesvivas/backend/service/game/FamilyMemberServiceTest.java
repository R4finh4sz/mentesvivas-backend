package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.FamilyGalleryResponse;
import com.mentesvivas.backend.dto.game.RegisterFamilyMemberRequest;
import com.mentesvivas.backend.entity.game.FamilyMember;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.game.FamilyMemberRepository;
import com.mentesvivas.backend.service.link.CaregiverPatientAccess;
import com.mentesvivas.backend.service.patient.PatientCareAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FamilyMemberServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-11-23T12:00:00Z");

    @Mock
    private PatientCareAccess access;
    @Mock
    private CaregiverPatientAccess caregiverAccess;
    @Mock
    private FamilyMemberRepository repository;
    @Mock
    private PhotoConsentGuard consent;

    private FamilyMemberService service;
    private User caregiver;

    @BeforeEach
    void setUp() {
        service = new FamilyMemberService(access, caregiverAccess, repository, consent,
                new RecognitionGamePolicy(), Clock.fixed(NOW, ZoneOffset.UTC));
        caregiver = new User();
        caregiver.setId(UUID.randomUUID());
    }

    private FamilyMember member(String name) {
        FamilyMember member = new FamilyMember();
        member.setId(UUID.randomUUID());
        member.setName(name);
        member.setKinship("filho");
        member.setPhotoUrl("https://img/x.png");
        return member;
    }

    @Test
    void registersTrimmedDataWithAuthorAndTime() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(caregiver);
        when(repository.save(any(FamilyMember.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.register(PATIENT, new RegisterFamilyMemberRequest("  Ana  ", " filha ",
                " https://img/ana.png ", "  "));

        ArgumentCaptor<FamilyMember> saved = ArgumentCaptor.forClass(FamilyMember.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getName()).isEqualTo("Ana");
        assertThat(saved.getValue().getKinship()).isEqualTo("filha");
        assertThat(saved.getValue().getPhotoUrl()).isEqualTo("https://img/ana.png");
        assertThat(saved.getValue().getAudioUrl()).isNull();
        assertThat(saved.getValue().getCreatedBy()).isEqualTo(caregiver.getId());
        assertThat(saved.getValue().getCreatedAt()).isEqualTo(NOW);
        assertThat(response.name()).isEqualTo("Ana");
    }

    @Test
    void doesNotRegisterWithoutPhotoConsent() {
        when(caregiverAccess.requireRepresentativeOf(PATIENT)).thenReturn(caregiver);
        doThrow(new AccessDeniedException("sem consentimento")).when(consent).requireConsent(PATIENT);

        assertThatThrownBy(() -> service.register(PATIENT,
                new RegisterFamilyMemberRequest("Ana", "filha", "https://img/ana.png", null)))
                .isInstanceOf(AccessDeniedException.class);
        verify(repository, never()).save(any());
    }

    @Test
    void galleryStaysLockedBelowFourPeople() {
        when(repository.findByPatientIdOrderByNameAsc(PATIENT))
                .thenReturn(List.of(member("Ana"), member("Bia"), member("Caio")));

        FamilyGalleryResponse gallery = service.gallery(PATIENT);

        assertThat(gallery.members()).hasSize(3);
        assertThat(gallery.recognitionGameUnlocked()).isFalse();
        assertThat(gallery.guidance()).isNotBlank();
    }

    @Test
    void galleryUnlocksWithFourPeople() {
        when(repository.findByPatientIdOrderByNameAsc(PATIENT))
                .thenReturn(List.of(member("Ana"), member("Bia"), member("Caio"), member("Davi")));

        FamilyGalleryResponse gallery = service.gallery(PATIENT);

        assertThat(gallery.recognitionGameUnlocked()).isTrue();
        assertThat(gallery.minimumForRecognitionGame()).isEqualTo(4);
        assertThat(gallery.guidance()).isNull();
    }

    @Test
    void removesAnExistingPerson() {
        FamilyMember existing = member("Ana");
        when(repository.findByIdAndPatientId(existing.getId(), PATIENT)).thenReturn(Optional.of(existing));

        service.remove(PATIENT, existing.getId());

        verify(caregiverAccess).requireRepresentativeOf(PATIENT);
        verify(repository).delete(existing);
    }

    @Test
    void removingAnUnknownPersonFails() {
        UUID id = UUID.randomUUID();
        when(repository.findByIdAndPatientId(id, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(PATIENT, id)).isInstanceOf(ResourceNotFoundException.class);
    }
}
