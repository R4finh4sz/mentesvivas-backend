package com.mentesvivas.backend.dto.link;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record InviteByEmailRequest(
        @NotBlank @Email @Size(max = 254) String email
) {

    public InviteByEmailRequest {
        email = Normalization.email(email);
    }
}
