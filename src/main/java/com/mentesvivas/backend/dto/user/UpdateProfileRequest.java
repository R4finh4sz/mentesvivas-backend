package com.mentesvivas.backend.dto.user;

import com.mentesvivas.backend.util.Normalization;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateProfileRequest(
        @Size(max = 150) @Pattern(regexp = ".*\\S.*", message = "o nome nao pode ficar em branco") String fullName,
        @Past LocalDate birthDate,
        @Pattern(regexp = "^\\d{10,11}$", message = "informe DDD e numero, apenas digitos") String phone,
        @Email @Size(max = 254) String email,
        @Size(max = 500) @Pattern(regexp = PhotoUrl.REGEX, message = PhotoUrl.MESSAGE) String photoUrl
) {

    public UpdateProfileRequest {
        fullName = Normalization.name(fullName);
        phone = Normalization.phone(phone);
        email = Normalization.email(email);
        photoUrl = PhotoUrl.normalize(photoUrl);
    }

    public UpdateProfileRequest(String fullName, LocalDate birthDate, String phone, String email) {
        this(fullName, birthDate, phone, email, null);
    }
}
