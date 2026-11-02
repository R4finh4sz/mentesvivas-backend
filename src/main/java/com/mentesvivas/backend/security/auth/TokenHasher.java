package com.mentesvivas.backend.security.auth;

import com.mentesvivas.backend.exception.config.ConfiguracaoInvalidaException;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
public class TokenHasher {

    public String hash(String valor) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(valor.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new ConfiguracaoInvalidaException("SHA-256 indisponivel nesta JVM", e);
        }
    }
}
