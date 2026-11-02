package com.mentesvivas.backend.repository.termos;

import com.mentesvivas.backend.entity.termos.TermosVersao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TermosVersaoRepository extends JpaRepository<TermosVersao, Integer> {

    Optional<TermosVersao> findTopByOrderByVersaoDesc();

    List<TermosVersao> findAllByOrderByVersaoAsc();
}
