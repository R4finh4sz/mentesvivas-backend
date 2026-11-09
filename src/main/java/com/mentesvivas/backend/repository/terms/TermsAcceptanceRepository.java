package com.mentesvivas.backend.repository.terms;

import com.mentesvivas.backend.entity.terms.TermsAcceptance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TermsAcceptanceRepository extends JpaRepository<TermsAcceptance, UUID> {

    boolean existsByUserIdAndVersion(UUID userId, Integer version);

    Optional<TermsAcceptance> findByUserIdAndVersion(UUID userId, Integer version);

    List<TermsAcceptance> findByUserIdOrderByVersionAsc(UUID userId);
}
