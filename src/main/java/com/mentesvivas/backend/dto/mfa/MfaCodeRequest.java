package com.mentesvivas.backend.dto.mfa;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaCodeRequest(
        @NotBlank @Pattern(regexp = "^\\d{6}$", message = "o codigo possui seis digitos") String code
) {

    public MfaCodeRequest {
        code = Normalization.token(code);
    }
}
