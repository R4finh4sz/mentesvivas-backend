package com.mentesvivas.backend.service.termos;

import com.mentesvivas.backend.dto.termos.PublicarTermosRequest;
import com.mentesvivas.backend.dto.termos.TermosResponse;
import com.mentesvivas.backend.entity.termos.TermosVersao;
import com.mentesvivas.backend.exception.termos.TermosNaoPublicadosException;
import com.mentesvivas.backend.mapper.termos.TermosMapper;
import com.mentesvivas.backend.repository.termos.TermosVersaoRepository;
import com.mentesvivas.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TermosService {

    private final TermosVersaoRepository termosVersaoRepository;
    private final CurrentUser currentUser;

    public TermosResponse atual() {
        return TermosMapper.toResponse(versaoAtual());
    }

    public List<TermosResponse> historico() {
        return termosVersaoRepository.findAllByOrderByVersaoAsc().stream()
                .map(TermosMapper::toResponse)
                .toList();
    }


    @Transactional
    public TermosResponse publicar(PublicarTermosRequest request) {
        int proximaVersao = termosVersaoRepository.findTopByOrderByVersaoDesc()
                .map(termos -> termos.getVersao() + 1)
                .orElse(1);
        TermosVersao termos = new TermosVersao(proximaVersao, request.titulo().strip(), request.conteudo(),
                currentUser.get().getId(), Instant.now());
        return TermosMapper.toResponse(termosVersaoRepository.saveAndFlush(termos));
    }

    public TermosVersao versaoAtual() {
        return termosVersaoRepository.findTopByOrderByVersaoDesc()
                .orElseThrow(TermosNaoPublicadosException::new);
    }
}
