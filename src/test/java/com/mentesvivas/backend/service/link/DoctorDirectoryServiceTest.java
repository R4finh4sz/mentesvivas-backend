package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DoctorDirectoryServiceTest {

    @Mock
    private CurrentUser currentUser;
    @Mock
    private UserRepository userRepository;

    private DoctorDirectoryService service;
    private User caller;

    @BeforeEach
    void setUp() {
        service = new DoctorDirectoryService(currentUser, userRepository);
        caller = new User();
        caller.setRole(Role.PATIENT);
        when(currentUser.get()).thenReturn(caller);
    }

    @Test
    void shortOrEmptyQueriesDoNotSearch() {
        assertThat(service.search("ab")).isEmpty();
        assertThat(service.search(null)).isEmpty();
        assertThat(service.search("%_%")).isEmpty();

        verify(userRepository, never()).searchVerifiedDoctors(any(), any());
    }

    @Test
    void wildcardsAreRemovedAndResultsAreMapped() {
        User doctor = new User();
        doctor.setId(UUID.randomUUID());
        doctor.setFullName("Dr. Paulo");
        doctor.setCrm("123456");
        doctor.setCrmState("SP");
        when(userRepository.searchVerifiedDoctors(eq("paulo"), any(Pageable.class))).thenReturn(List.of(doctor));

        var result = service.search(" pa%ulo ");

        assertThat(result).singleElement().satisfies(item -> {
            assertThat(item.fullName()).isEqualTo("Dr. Paulo");
            assertThat(item.crm()).isEqualTo("123456");
        });
    }

    @Test
    void onlyPatientsAndCaregiversSearch() {
        caller.setRole(Role.DOCTOR);

        assertThatThrownBy(() -> service.search("paulo")).isInstanceOf(AccessDeniedException.class);
    }
}
