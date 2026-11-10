package com.mentesvivas.backend.service.link;

import com.mentesvivas.backend.dto.link.DoctorSummaryResponse;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.mapper.link.LinkMapper;
import com.mentesvivas.backend.repository.user.UserRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DoctorDirectoryService {

    private static final int MIN_QUERY_LENGTH = 3;
    private static final int MAX_RESULTS = 20;

    private final CurrentUser currentUser;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<DoctorSummaryResponse> search(String query) {
        Role role = currentUser.get().getRole();
        if (role != Role.PATIENT && role != Role.CAREGIVER) {
            throw new AccessDeniedException("Acesso restrito a pacientes e cuidadores.");
        }
        String term = query == null ? "" : query.replaceAll("[%_\\\\]", "").strip();
        if (term.length() < MIN_QUERY_LENGTH) {
            return List.of();
        }
        return userRepository.searchVerifiedDoctors(term, Pageable.ofSize(MAX_RESULTS)).stream()
                .map(LinkMapper::toDoctor)
                .toList();
    }
}
