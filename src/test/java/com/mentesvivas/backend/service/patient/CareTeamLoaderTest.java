package com.mentesvivas.backend.service.patient;

import com.mentesvivas.backend.entity.link.CaregiverPatientLink;
import com.mentesvivas.backend.entity.link.DoctorPatientLink;
import com.mentesvivas.backend.entity.link.LinkStatus;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.link.CaregiverPatientLinkRepository;
import com.mentesvivas.backend.repository.link.DoctorPatientLinkRepository;
import com.mentesvivas.backend.repository.user.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CareTeamLoaderTest {

    private static final UUID PATIENT_ID = UUID.randomUUID();

    @Mock
    private DoctorPatientLinkRepository doctorLinkRepository;
    @Mock
    private CaregiverPatientLinkRepository caregiverLinkRepository;
    @Mock
    private UserRepository userRepository;

    private CareTeamLoader loader;

    @BeforeEach
    void setUp() {
        loader = new CareTeamLoader(doctorLinkRepository, caregiverLinkRepository, userRepository);
    }

    private static User user(UUID id, String name) {
        User user = new User() {
            @Override
            public UUID getId() {
                return id;
            }
        };
        user.setFullName(name);
        return user;
    }

    @Test
    void returnsTheDoctorsAndCaregiversWithActiveLinkSortedByName() {
        UUID doctorId = UUID.randomUUID();
        UUID firstCaregiver = UUID.randomUUID();
        UUID secondCaregiver = UUID.randomUUID();
        when(doctorLinkRepository.findByPatientIdAndStatus(PATIENT_ID, LinkStatus.ACTIVE))
                .thenReturn(List.of(new DoctorPatientLink(doctorId, PATIENT_ID, LinkStatus.ACTIVE, Instant.now())));
        when(caregiverLinkRepository.findByPatientIdAndStatus(PATIENT_ID, LinkStatus.ACTIVE)).thenReturn(List.of(
                new CaregiverPatientLink(secondCaregiver, PATIENT_ID, LinkStatus.ACTIVE),
                new CaregiverPatientLink(firstCaregiver, PATIENT_ID, LinkStatus.ACTIVE)));
        User doctor = user(doctorId, "Dr. Paulo");
        doctor.setCrm("123456");
        when(userRepository.findAllById(List.of(doctorId))).thenReturn(List.of(doctor));
        when(userRepository.findAllById(List.of(secondCaregiver, firstCaregiver))).thenReturn(List.of(
                user(secondCaregiver, "Zelia"), user(firstCaregiver, "Ana")));

        var team = loader.load(PATIENT_ID);

        assertThat(team.doctors()).singleElement().satisfies(d -> {
            assertThat(d.fullName()).isEqualTo("Dr. Paulo");
            assertThat(d.crm()).isEqualTo("123456");
        });
        assertThat(team.caregivers()).extracting("fullName").containsExactly("Ana", "Zelia");
    }

    @Test
    void returnsEmptyListsWhenThereAreNoActiveLinks() {
        when(doctorLinkRepository.findByPatientIdAndStatus(PATIENT_ID, LinkStatus.ACTIVE)).thenReturn(List.of());
        when(caregiverLinkRepository.findByPatientIdAndStatus(PATIENT_ID, LinkStatus.ACTIVE)).thenReturn(List.of());
        when(userRepository.findAllById(List.of())).thenReturn(List.of());

        var team = loader.load(PATIENT_ID);

        assertThat(team.doctors()).isEmpty();
        assertThat(team.caregivers()).isEmpty();
    }
}
