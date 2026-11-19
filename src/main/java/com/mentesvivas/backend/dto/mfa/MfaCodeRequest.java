package com.mentesvivas.backend.dto.mfa;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaCodeRequest(
        @NotBlank @Pattern(regexp = "^(\\d{6}|\\d{10})$", message = "o codigo possui seis digitos (ou dez, no codigo de backup)") String code
) {

    public MfaCodeRequest {
        code = Normalization.token(code);
    }
}
