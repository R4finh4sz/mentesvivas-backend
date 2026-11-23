package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.user.User;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Component
@ConditionalOnProperty(name = "app.signature.mode", havingValue = "SIMULATED")
public class SimulatedDocumentSigner implements DocumentSigner {

    @Override
    public SignatureResult sign(byte[] content, User signer) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(content);
            return new SignatureResult("SIMULADA-" + HexFormat.of().formatHex(digest).substring(0, 32), true);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
