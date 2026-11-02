package com.mentesvivas.backend.dto.termos;

import java.time.Instant;

public record AceiteTermosResponse(
        Integer versao,
        Instant aceitoEm
) {
}
