package com.mentesvivas.backend.service.clinical;

import com.mentesvivas.backend.dto.clinical.DeclareDiseaseRequest;
import com.mentesvivas.backend.dto.patient.AddAllergyRequest;
import com.mentesvivas.backend.dto.patient.SetAllergyStatusRequest;
import com.mentesvivas.backend.entity.patient.Allergy;
import com.mentesvivas.backend.entity.patient.AllergyCategory;
import com.mentesvivas.backend.entity.patient.AllergySeverity;
import com.mentesvivas.backend.entity.patient.AllergyStatus;
import com.mentesvivas.backend.entity.patient.ChangeType;
import com.mentesvivas.backend.entity.patient.PatientProfile;
import com.mentesvivas.backend.entity.patient.SensitiveField;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ConflictException;
import com.mentesvivas.backend.exception.common.ResourceNotFoundException;
import com.mentesvivas.backend.repository.patient.AllergyRepository;
import com.mentesvivas.backend.service.patient.PatientProfileLoader;
import com.mentesvivas.backend.service.patient.SensitiveChangeRecorder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorClinicalServiceTest {

    private static final UUID PATIENT = UUID.randomUUID();
    private static final UUID ALLERGY = UUID.randomUUID();

    @Mock
    private DoctorPatientAccess access;
    @Mock
    private AllergyRepository allergyRepository;
    @Mock
    private PatientProfileLoader profileLoader;
    @Mock
    private SensitiveChangeRecorder changeRecorder;

    private DoctorClinicalService service;
    private User doctor;
    private PatientProfile profile;

    @BeforeEach
    void setUp() {
        service = new DoctorClinicalService(access, allergyRepository, profileLoader, changeRecorder);
        doctor = new User();
        doctor.setId(UUID.randomUUID());
        profile = new PatientProfile(PATIENT);
        lenient().when(access.requireDoctorOf(PATIENT)).thenReturn(doctor);
    }

    private AddAllergyRequest request() {
        return new AddAllergyRequest(AllergyCategory.MEDICATION, "Dipirona", AllergySeverity.SEVERE);
    }

    @Test
    void addingAnAllergyMarksItInformedAndRecordsTheDoctorAsAuthor() {
        when(allergyRepository.save(any(Allergy.class))).thenAnswer(call -> call.getArgument(0));
        when(profileLoader.getOrCreate(PATIENT)).thenReturn(profile);

        var response = service.addAllergy(PATIENT, request());

        assertThat(response.description()).isEqualTo("Dipirona");
        assertThat(profile.getAllergyStatus()).isEqualTo(AllergyStatus.INFORMED);
        verify(changeRecorder).record(eq(PATIENT), eq(SensitiveField.ALLERGY), eq(ChangeType.CREATED),
                anyString(), eq(doctor.getId()));
    }

    @Test
    void updatingChangesTheAllergyAndRecordsIt() {
        Allergy allergy = new Allergy();
        allergy.setPatientId(PATIENT);
        allergy.setCategory(AllergyCategory.FOOD);
        allergy.setDescription("Amendoim");
        allergy.setSeverity(AllergySeverity.MILD);
        when(allergyRepository.findByIdAndPatientId(ALLERGY, PATIENT)).thenReturn(Optional.of(allergy));

        service.updateAllergy(PATIENT, ALLERGY, request());

        assertThat(allergy.getDescription()).isEqualTo("Dipirona");
        assertThat(allergy.getSeverity()).isEqualTo(AllergySeverity.SEVERE);
        verify(changeRecorder).record(eq(PATIENT), eq(SensitiveField.ALLERGY), eq(ChangeType.UPDATED),
                anyString(), eq(doctor.getId()));
    }

    @Test
    void allergyOfAnotherPatientIsNotFound() {
        when(allergyRepository.findByIdAndPatientId(ALLERGY, PATIENT)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.updateAllergy(PATIENT, ALLERGY, request()))
                .isInstanceOf(ResourceNotFoundException.class);
        assertThatThrownBy(() -> service.removeAllergy(PATIENT, ALLERGY))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removingTheLastAllergyReturnsTheStatusToNotInformed() {
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.LATEX);
        profile.setAllergyStatus(AllergyStatus.INFORMED);
        when(allergyRepository.findByIdAndPatientId(ALLERGY, PATIENT)).thenReturn(Optional.of(allergy));
        when(allergyRepository.existsByPatientId(PATIENT)).thenReturn(false);
        when(profileLoader.getOrCreate(PATIENT)).thenReturn(profile);

        service.removeAllergy(PATIENT, ALLERGY);

        verify(allergyRepository).delete(allergy);
        assertThat(profile.getAllergyStatus()).isEqualTo(AllergyStatus.NOT_INFORMED);
        verify(changeRecorder).record(eq(PATIENT), eq(SensitiveField.ALLERGY), eq(ChangeType.DELETED),
                anyString(), eq(doctor.getId()));
    }

    @Test
    void removingOneOfSeveralKeepsTheStatus() {
        Allergy allergy = new Allergy();
        allergy.setCategory(AllergyCategory.LATEX);
        profile.setAllergyStatus(AllergyStatus.INFORMED);
        when(allergyRepository.findByIdAndPatientId(ALLERGY, PATIENT)).thenReturn(Optional.of(allergy));
        when(allergyRepository.existsByPatientId(PATIENT)).thenReturn(true);

        service.removeAllergy(PATIENT, ALLERGY);

        assertThat(profile.getAllergyStatus()).isEqualTo(AllergyStatus.INFORMED);
    }

    @Test
    void noKnownAllergiesCannotBeSetWhileThereAreAllergies() {
        when(allergyRepository.existsByPatientId(PATIENT)).thenReturn(true);

        assertThatThrownBy(() -> service.setAllergyStatus(PATIENT, new SetAllergyStatusRequest(AllergyStatus.NONE_KNOWN)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void doctorCanDeclareNoKnownAllergies() {
        when(allergyRepository.existsByPatientId(PATIENT)).thenReturn(false);
        when(profileLoader.getOrCreate(PATIENT)).thenReturn(profile);

        var status = service.setAllergyStatus(PATIENT, new SetAllergyStatusRequest(AllergyStatus.NONE_KNOWN));

        assertThat(status).isEqualTo(AllergyStatus.NONE_KNOWN);
        verify(changeRecorder).record(eq(PATIENT), eq(SensitiveField.ALLERGY_STATUS), eq(ChangeType.UPDATED),
                anyString(), eq(doctor.getId()));
    }

    @Test
    void validatingTheDiseaseReplacesTheDeclaredOneAndMarksItValidated() {
        profile.setDeclaredDisease("Esquecimento");
        when(profileLoader.getOrCreate(PATIENT)).thenReturn(profile);

        service.validateDisease(PATIENT, new DeclareDiseaseRequest("  Alzheimer  "));

        assertThat(profile.getDeclaredDisease()).isEqualTo("Alzheimer");
        assertThat(profile.isDeclaredDiseaseValidated()).isTrue();
        verify(changeRecorder).record(eq(PATIENT), eq(SensitiveField.DECLARED_DISEASE), eq(ChangeType.UPDATED),
                anyString(), eq(doctor.getId()));
    }

    @Test
    void withoutAccessNothingIsChanged() {
        UUID other = UUID.randomUUID();
        when(access.requireDoctorOf(other)).thenThrow(new AccessDeniedException("sem vinculo"));

        assertThatThrownBy(() -> service.addAllergy(other, request())).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> service.validateDisease(other, new DeclareDiseaseRequest("X")))
                .isInstanceOf(AccessDeniedException.class);
        verify(allergyRepository, never()).save(any());
        verify(changeRecorder, never()).record(any(), any(), any(), anyString(), any());
    }
}
