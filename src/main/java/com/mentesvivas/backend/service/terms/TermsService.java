package com.mentesvivas.backend.service.terms;

import com.mentesvivas.backend.dto.terms.PublishTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsResponse;
import com.mentesvivas.backend.entity.terms.TermsAudience;
import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.terms.TermsVersion;
import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.exception.terms.TermsNotPublishedException;
import com.mentesvivas.backend.mapper.terms.TermsMapper;
import com.mentesvivas.backend.repository.terms.TermsVersionRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermsService {

    private final TermsVersionRepository termsVersionRepository;
    private final CurrentUser currentUser;

    public TermsResponse current(TermsType type, Role role) {
        TermsType wanted = type == null ? TermsType.TERMS_OF_USE : type;
        Optional<TermsVersion> found = role == null
                ? termsVersionRepository.findByTypeOrderByVersionDesc(wanted).stream()
                        .filter(terms -> terms.getAudience() == TermsAudience.ALL).findFirst()
                : currentFor(wanted, role);
        return TermsMapper.toResponse(found.orElseThrow(TermsNotPublishedException::new));
    }

    public List<TermsResponse> currentForSignup(Role role) {
        return applicableCurrent(role).stream().map(TermsMapper::toResponse).toList();
    }

    public List<TermsResponse> history() {
        return termsVersionRepository.findAllByOrderByVersionAsc().stream()
                .map(TermsMapper::toResponse)
                .toList();
    }

    public boolean hasAny(TermsType type) {
        return termsVersionRepository.existsByType(type);
    }

    public Optional<TermsVersion> currentFor(TermsType type, Role role) {
        return termsVersionRepository.findByTypeOrderByVersionDesc(type).stream()
                .filter(terms -> terms.appliesTo(role))
                .findFirst();
    }

    public List<TermsVersion> applicableCurrent(Role role) {
        return Arrays.stream(TermsType.values())
                .map(type -> currentFor(type, role))
                .flatMap(Optional::stream)
                .toList();
    }

    @Transactional
    public TermsResponse publish(PublishTermsRequest request) {
        int nextVersion = termsVersionRepository.findTopByOrderByVersionDesc()
                .map(terms -> terms.getVersion() + 1)
                .orElse(1);
        TermsVersion terms = new TermsVersion(nextVersion, request.typeOrDefault(), request.audienceOrDefault(),
                request.roles(), request.title().strip(), request.content(),
                currentUser.get().getId(), Instant.now());
        return TermsMapper.toResponse(termsVersionRepository.saveAndFlush(terms));
    }
}
