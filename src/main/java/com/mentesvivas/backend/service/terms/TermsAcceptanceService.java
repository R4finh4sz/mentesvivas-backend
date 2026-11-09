package com.mentesvivas.backend.service.terms;

import com.mentesvivas.backend.dto.terms.AcceptTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsAcceptanceResponse;
import com.mentesvivas.backend.entity.terms.TermsAcceptance;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.terms.TermsVersionChangedException;
import com.mentesvivas.backend.mapper.terms.TermsMapper;
import com.mentesvivas.backend.repository.terms.TermsAcceptanceRepository;
import com.mentesvivas.backend.repository.terms.TermsVersionRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsAcceptanceService {

    private final TermsService termsService;
    private final TermsVersionRepository termsVersionRepository;
    private final TermsAcceptanceRepository termsAcceptanceRepository;
    private final CurrentUser currentUser;

    public void validateCurrentVersion(Integer version) {
        if (!termsService.currentVersion().getVersion().equals(version)) {
            throw new TermsVersionChangedException();
        }
    }

    @Transactional
    public TermsAcceptance register(User user, Integer version) {
        validateCurrentVersion(version);
        return termsAcceptanceRepository.findByUserIdAndVersion(user.getId(), version)
                .orElseGet(() -> termsAcceptanceRepository.save(new TermsAcceptance(user, version, Instant.now())));
    }

    @Transactional
    public TermsAcceptanceResponse accept(AcceptTermsRequest request) {
        return TermsMapper.toResponse(register(currentUser.get(), request.version()));
    }

    public List<TermsAcceptanceResponse> currentUserAcceptances() {
        return termsAcceptanceRepository.findByUserIdOrderByVersionAsc(currentUser.get().getId()).stream()
                .map(TermsMapper::toResponse)
                .toList();
    }

    public boolean acceptedCurrentVersion(User user) {
        if (user.getRole() == Role.ADMINISTRATOR) {
            return true;
        }
        return termsVersionRepository.findTopByOrderByVersionDesc()
                .map(terms -> termsAcceptanceRepository.existsByUserIdAndVersion(user.getId(), terms.getVersion()))
                .orElse(true);
    }
}
