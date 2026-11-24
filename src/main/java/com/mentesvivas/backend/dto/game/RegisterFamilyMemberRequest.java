package com.mentesvivas.backend.dto.game;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterFamilyMemberRequest(
        @NotBlank(message = "informe o nome da pessoa")
        @Size(max = 100, message = "o nome aceita ate 100 caracteres") String name,
        @NotBlank(message = "informe o grau de parentesco")
        @Size(max = 50, message = "o parentesco aceita ate 50 caracteres") String kinship,
        @NotBlank(message = "informe o link da foto")
        @Size(max = 500, message = "o link da foto aceita ate 500 caracteres")
        @Pattern(regexp = "^https://\\S+$", message = "informe o link da foto comecando com https://") String photoUrl,
        @Size(max = 500, message = "o link do audio aceita ate 500 caracteres")
        @Pattern(regexp = "^(https://\\S+)?$", message = "informe o link do audio comecando com https://")
        String audioUrl
) {
}
