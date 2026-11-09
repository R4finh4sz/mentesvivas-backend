package com.mentesvivas.backend.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Normalization {

    private Normalization() {
    }

    public static String email(String value) {
        return value == null ? null : unicode(value).strip().toLowerCase(Locale.ROOT);
    }

    public static String name(String value) {
        return value == null ? null : unicode(value).strip().replaceAll("\\s+", " ");
    }

    public static String cpf(String value) {
        return value == null ? null : value.replaceAll("[\\s.\\-]", "");
    }

    public static String phone(String value) {
        return value == null ? null : value.replaceAll("[\\s().\\-]", "");
    }

    public static String cep(String value) {
        return value == null ? null : value.replaceAll("[\\s.\\-]", "");
    }

    public static String crm(String value) {
        return value == null ? null : value.replaceAll("[\\s.\\-]", "");
    }

    public static String state(String value) {
        return value == null ? null : value.strip().toUpperCase(Locale.ROOT);
    }

    public static String token(String value) {
        return value == null ? null : value.strip();
    }

    private static String unicode(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC);
    }
}
