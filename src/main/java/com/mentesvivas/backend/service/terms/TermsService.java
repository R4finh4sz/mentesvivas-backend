package com.mentesvivas.backend.service.terms;

import com.mentesvivas.backend.dto.terms.PublishTermsRequest;
import com.mentesvivas.backend.dto.terms.TermsResponse;
import com.mentesvivas.backend.entity.terms.TermsVersion;
import com.mentesvivas.backend.exception.terms.TermsNotPublishedException;
import com.mentesvivas.backend.mapper.terms.TermsMapper;
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
public class TermsService {

    private final TermsVersionRepository termsVersionRepository;
    private final CurrentUser currentUser;

    public TermsResponse current() {
        return TermsMapper.toResponse(currentVersion());
    }

    public List<TermsResponse> history() {
        return termsVersionRepository.findAllByOrderByVersionAsc().stream()
                .map(TermsMapper::toResponse)
                .toList();
    }


    @Transactional
    public TermsResponse publish(PublishTermsRequest request) {
        int nextVersion = termsVersionRepository.findTopByOrderByVersionDesc()
                .map(terms -> terms.getVersion() + 1)
                .orElse(1);
        TermsVersion terms = new TermsVersion(nextVersion, request.title().strip(), request.content(),
                currentUser.get().getId(), Instant.now());
        return TermsMapper.toResponse(termsVersionRepository.saveAndFlush(terms));
    }

    public TermsVersion currentVersion() {
        return termsVersionRepository.findTopByOrderByVersionDesc()
                .orElseThrow(TermsNotPublishedException::new);
    }
}
