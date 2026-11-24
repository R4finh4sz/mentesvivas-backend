package com.mentesvivas.backend.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SaveBingoSoundRequest(
        @NotBlank(message = "informe o nome do instrumento")
        @Size(max = 60, message = "o instrumento aceita ate 60 caracteres") String instrument,
        @NotBlank(message = "informe o link do audio")
        @Size(max = 500, message = "o link do audio aceita ate 500 caracteres")
        @Pattern(regexp = "^https://\\S+$", message = "informe o link do audio comecando com https://")
        String audioUrl,
        Boolean active
) {

    public boolean isActive() {
        return active == null || active;
    }
}
