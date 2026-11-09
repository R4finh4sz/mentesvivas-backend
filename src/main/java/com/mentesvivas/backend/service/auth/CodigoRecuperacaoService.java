package com.mentesvivas.backend.service.auth;

import com.mentesvivas.backend.cache.ContadorTemporario;
import com.mentesvivas.backend.cache.ValorTemporario;
import com.mentesvivas.backend.config.RecuperacaoSenhaProperties;
import com.mentesvivas.backend.exception.auth.CodigoRecuperacaoInvalidoException;
import com.mentesvivas.backend.security.auth.TokenHasher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CodigoRecuperacaoService {

    private static final String PREFIXO_CODIGO = "recuperacao:codigo:";
    private static final String PREFIXO_TENTATIVAS = "recuperacao:tentativas:";
    private static final String PREFIXO_ENVIOS = "recuperacao:envios:";

    private final SecureRandom secureRandom = new SecureRandom();
    private final ContadorTemporario contador;
    private final ValorTemporario valores;
    private final RecuperacaoSenhaProperties properties;
    private final TokenHasher hasher;

    public Optional<String> gerar(String email) {
        String chave = hasher.hash(email);
        long envios = contador.incrementar(PREFIXO_ENVIOS + chave, properties.janelaEnvios());
        if (envios > properties.maxEnvios()) {
            return Optional.empty();
        }
        String codigo = "%06d".formatted(secureRandom.nextInt(1_000_000));
        valores.gravar(PREFIXO_CODIGO + chave, hashDoCodigo(chave, codigo), properties.validadeCodigo());
        contador.remover(PREFIXO_TENTATIVAS + chave);
        return Optional.of(codigo);
    }

    public void validarEConsumir(String email, String codigo) {
        String chave = hasher.hash(email);
        long tentativas = contador.incrementar(PREFIXO_TENTATIVAS + chave, properties.validadeCodigo());
        if (tentativas > properties.maxTentativas()) {
            valores.remover(PREFIXO_CODIGO + chave);
            throw new CodigoRecuperacaoInvalidoException();
        }
        String esperado = valores.obter(PREFIXO_CODIGO + chave).orElseThrow(CodigoRecuperacaoInvalidoException::new);
        boolean confere = MessageDigest.isEqual(
                esperado.getBytes(StandardCharsets.UTF_8),
                hashDoCodigo(chave, codigo).getBytes(StandardCharsets.UTF_8));
        if (!confere) {
            throw new CodigoRecuperacaoInvalidoException();
        }
        valores.remover(PREFIXO_CODIGO + chave);
        contador.remover(PREFIXO_TENTATIVAS + chave);
    }

    private String hashDoCodigo(String chave, String codigo) {
        return hasher.hash(chave + ":" + codigo);
    }
}
