package com.mentesvivas.backend.service.patient.hydration;

import com.mentesvivas.backend.config.HydrationProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class HydrationConditionClassifier {

    private final HydrationProperties properties;

    public HydrationCondition classify(String declaredDisease) {
        if (declaredDisease == null || declaredDisease.isBlank()) {
            return HydrationCondition.NONE;
        }
        String text = plain(declaredDisease);
        if (matches(text, properties.renalKeywords()) || matches(text, properties.cardiacKeywords())) {
            return HydrationCondition.MEDICAL_GOAL_REQUIRED;
        }
        if (matches(text, properties.consultKeywords())) {
            return HydrationCondition.CONSULT_DOCTOR;
        }
        return HydrationCondition.NONE;
    }

    private boolean matches(String text, List<String> keywords) {
        return keywords != null && keywords.stream().map(this::plain).anyMatch(text::contains);
    }

    private String plain(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
