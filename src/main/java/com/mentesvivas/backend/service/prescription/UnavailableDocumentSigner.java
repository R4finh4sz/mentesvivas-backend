package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.exception.common.ServiceUnavailableException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.signature.mode", havingValue = "UNAVAILABLE", matchIfMissing = true)
public class UnavailableDocumentSigner implements DocumentSigner {

    @Override
    public SignatureResult sign(byte[] content, User signer) {
        throw new ServiceUnavailableException(
                "O servico de assinatura digital ICP-Brasil nao esta disponivel. Tente novamente.", null);
    }
}
