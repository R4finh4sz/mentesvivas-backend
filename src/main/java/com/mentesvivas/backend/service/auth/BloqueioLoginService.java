package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.cache.ContadorTemporario;
import com.mentesvivas.backend.cache.ValorTemporario;
import com.mentesvivas.backend.config.LoginBloqueioProperties;
import com.mentesvivas.backend.exception.auth.ContaBloqueadaException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BloqueioLoginService {

    private static final String PREFIXO_FALHAS = "login:falhas:";
    private static final String PREFIXO_BLOQUEIO = "login:bloqueio:";

    private final ContadorTemporario contador;
    private final ValorTemporario valores;
    private final LoginBloqueioProperties properties;
    private final TokenHasher hasher;

    public void exigirDesbloqueado(String email) {
        valores.tempoRestante(PREFIXO_BLOQUEIO + hasher.hash(email))
                .ifPresent(restante -> {
                    throw new ContaBloqueadaException(restante);
                });
    }

    public void registrarFalha(String email) {
        String chave = hasher.hash(email);
        long falhas = contador.incrementar(PREFIXO_FALHAS + chave, properties.janelaTentativas());
        if (falhas >= properties.maxTentativas()) {
            valores.gravar(PREFIXO_BLOQUEIO + chave, "1", properties.duracaoBloqueio());
            contador.remover(PREFIXO_FALHAS + chave);
        }
    }

    public void registrarSucesso(String email) {
        contador.remover(PREFIXO_FALHAS + hasher.hash(email));
    }
}
