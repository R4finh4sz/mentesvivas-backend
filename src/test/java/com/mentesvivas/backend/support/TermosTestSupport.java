package com.mentesvivas.backend.support;

import com.mentesvivas.backend.entity.termos.TermosVersao;
import com.mentesvivas.backend.repository.termos.TermosVersaoRepository;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
public class TermosTestSupport {

    public static final String ADMIN_EMAIL = "admin@mentesvivas.test";
    public static final String ADMIN_SENHA = "senhaAdminTeste123";

    private final TermosVersaoRepository termosVersaoRepository;
    private final UsuarioRepository usuarioRepository;

    public TermosTestSupport(TermosVersaoRepository termosVersaoRepository, UsuarioRepository usuarioRepository) {
        this.termosVersaoRepository = termosVersaoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public int publicar() {
        int versao = termosVersaoRepository.findTopByOrderByVersaoDesc()
                .map(termos -> termos.getVersao() + 1)
                .orElse(1);
        var admin = usuarioRepository.findByEmail(ADMIN_EMAIL).orElseThrow();
        termosVersaoRepository.saveAndFlush(new TermosVersao(versao, "Termos v" + versao, "Conteudo", admin.getId(), Instant.now()));
        return versao;
    }
}
