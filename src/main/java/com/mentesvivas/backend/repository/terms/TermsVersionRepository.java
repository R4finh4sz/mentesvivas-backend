package com.mentesvivas.backend.repository.terms;

import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.terms.TermsVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermsVersionRepository extends JpaRepository<TermsVersion, Integer> {

    Optional<TermsVersion> findTopByOrderByVersionDesc();

    List<TermsVersion> findAllByOrderByVersionAsc();

    List<TermsVersion> findByTypeOrderByVersionDesc(TermsType type);

    boolean existsByType(TermsType type);
}
