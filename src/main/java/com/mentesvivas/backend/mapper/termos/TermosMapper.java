package com.mentesvivas.backend.mapper.termos;

import com.mentesvivas.backend.dto.termos.AceiteTermosResponse;
import com.mentesvivas.backend.dto.termos.TermosResponse;
import com.mentesvivas.backend.entity.termos.AceiteTermos;
import com.mentesvivas.backend.entity.termos.TermosVersao;

public final class TermosMapper {

    private TermosMapper() {
    }

    public static TermosResponse toResponse(TermosVersao termos) {
        return new TermosResponse(termos.getVersao(), termos.getTitulo(), termos.getConteudo(), termos.getPublicadoEm());
    }

    public static AceiteTermosResponse toResponse(AceiteTermos aceite) {
        return new AceiteTermosResponse(aceite.getVersao(), aceite.getAceitoEm());
    }
}
