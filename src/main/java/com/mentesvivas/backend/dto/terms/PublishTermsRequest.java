package com.mentesvivas.backend.dto.terms;

import com.mentesvivas.backend.entity.terms.TermsAudience;
import com.mentesvivas.backend.entity.terms.TermsType;
import com.mentesvivas.backend.entity.user.Role;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.Set;

public record PublishTermsRequest(
        @NotBlank @Size(max = 200) String title,
        @NotBlank String content,
        TermsType type,
        TermsAudience audience,
        Set<Role> roles
) {

    public PublishTermsRequest(String title, String content) {
        this(title, content, null, null, null);
    }

    public TermsType typeOrDefault() {
        return type == null ? TermsType.TERMS_OF_USE : type;
    }

    public TermsAudience audienceOrDefault() {
        return audience == null ? TermsAudience.ALL : audience;
    }

    @AssertTrue(message = "para publico especifico informe ao menos um perfil; para todos, nao informe perfis")
    public boolean isAudienceConsistent() {
        boolean hasRoles = roles != null && !roles.isEmpty();
        return audienceOrDefault() == TermsAudience.SPECIFIC ? hasRoles : !hasRoles;
    }

    @AssertTrue(message = "o administrador nao aceita termos")
    public boolean isRolesAcceptable() {
        return roles == null || !roles.contains(Role.ADMINISTRATOR);
    }
}
