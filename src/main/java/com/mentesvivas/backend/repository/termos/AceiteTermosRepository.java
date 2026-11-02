package com.mentesvivas.backend.repository.termos;

import com.mentesvivas.backend.entity.termos.AceiteTermos;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AceiteTermosRepository extends JpaRepository<AceiteTermos, UUID> {

    boolean existsByUsuarioIdAndVersao(UUID usuarioId, Integer versao);

    Optional<AceiteTermos> findByUsuarioIdAndVersao(UUID usuarioId, Integer versao);

    List<AceiteTermos> findByUsuarioIdOrderByVersaoAsc(UUID usuarioId);
}
