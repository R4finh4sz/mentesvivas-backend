package com.mentesvivas.backend.dto.termos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PublicarTermosRequest(
        @NotBlank @Size(max = 200) String titulo,
        @NotBlank String conteudo
) {
}
