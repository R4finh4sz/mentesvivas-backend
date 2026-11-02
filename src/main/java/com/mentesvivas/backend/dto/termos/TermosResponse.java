package com.mentesvivas.backend.dto.termos;

import java.time.Instant;

public record TermosResponse(
        Integer versao,
        String titulo,
        String conteudo,
        Instant publicadoEm
) {
}
