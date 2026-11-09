package com.mentesvivas.backend.dto.auth;

public record RecuperacaoSolicitadaResponse(
        String mensagem,
        long codigoExpiraEmSegundos
) {
}
