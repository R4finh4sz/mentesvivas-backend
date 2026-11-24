package com.mentesvivas.backend.config;

import com.mentesvivas.backend.entity.user.Role;
import com.mentesvivas.backend.security.HealthDataConsentAccess;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Map;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
class HealthDataConsentConfig implements WebMvcConfigurer {

    private final HealthDataConsentAccess consent;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
                if (!(request.getUserPrincipal() instanceof JwtAuthenticationToken token)) {
                    return true;
                }
                String role = token.getToken().getClaimAsString(JwtConfig.ROLE_CLAIM);
                if (Role.PATIENT.name().equals(role)) {
                    consent.requireActive(UUID.fromString(token.getName()));
                } else if ((Role.CAREGIVER.name().equals(role) || Role.DOCTOR.name().equals(role))
                        && isWrite(request.getMethod())
                        && request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE) instanceof Map<?, ?> vars
                        && vars.get("patientId") instanceof String patientId) {
                    requireIfUuid(patientId);
                }
                return true;
            }
        }).addPathPatterns("/api/**")
                .excludePathPatterns("/api/auth/**", "/api/public/**", "/api/terms", "/api/terms/signup",
                        "/api/terms/accept", "/api/patients/me/consents/health-data",
                        "/api/users/me/data-requests", "/api/patients/me/sos",
                        "/api/care/patients/*/alerts", "/api/care/patients/*/alerts/**");
    }

    private static boolean isWrite(String method) {
        return switch (method) {
            case "POST", "PUT", "PATCH", "DELETE" -> true;
            default -> false;
        };
    }

    private void requireIfUuid(String value) {
        UUID patientId;
        try {
            patientId = UUID.fromString(value);
        } catch (IllegalArgumentException ignored) {
            return; // The controller already validates the path parameter.
        }
        consent.requireActive(patientId);
    }
}
