package com.mentesvivas.backend.service.termos;

import com.mentesvivas.backend.dto.termos.AceitarTermosRequest;
import com.mentesvivas.backend.dto.termos.AceiteTermosResponse;
import com.mentesvivas.backend.entity.termos.AceiteTermos;
import com.mentesvivas.backend.entity.usuario.Role;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.termos.TermosVersaoAlteradaException;
import com.mentesvivas.backend.mapper.termos.TermosMapper;
import com.mentesvivas.backend.repository.termos.AceiteTermosRepository;
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
public class AceiteTermosService {

    private final TermosService termosService;
    private final TermosVersaoRepository termosVersaoRepository;
    private final AceiteTermosRepository aceiteTermosRepository;
    private final CurrentUser currentUser;

    public void validarVersaoAtual(Integer versao) {
        if (!termosService.versaoAtual().getVersao().equals(versao)) {
            throw new TermosVersaoAlteradaException();
        }
    }

    @Transactional
    public AceiteTermos registrar(Usuario usuario, Integer versao) {
        validarVersaoAtual(versao);
        return aceiteTermosRepository.findByUsuarioIdAndVersao(usuario.getId(), versao)
                .orElseGet(() -> aceiteTermosRepository.save(new AceiteTermos(usuario, versao, Instant.now())));
    }

    @Transactional
    public AceiteTermosResponse aceitar(AceitarTermosRequest request) {
        return TermosMapper.toResponse(registrar(currentUser.get(), request.versao()));
    }

    public List<AceiteTermosResponse> aceitesDoUsuarioAtual() {
        return aceiteTermosRepository.findByUsuarioIdOrderByVersaoAsc(currentUser.get().getId()).stream()
                .map(TermosMapper::toResponse)
                .toList();
    }

    public boolean aceitouVersaoAtual(Usuario usuario) {
        if (usuario.getRole() == Role.ADMINISTRADOR) {
            return true;
        }
        return termosVersaoRepository.findTopByOrderByVersaoDesc()
                .map(termos -> aceiteTermosRepository.existsByUsuarioIdAndVersao(usuario.getId(), termos.getVersao()))
                .orElse(true);
    }
}
