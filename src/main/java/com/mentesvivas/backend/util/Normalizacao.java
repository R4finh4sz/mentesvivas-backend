package com.mentesvivas.backend.util;

import java.text.Normalizer;
import java.util.Locale;

public final class Normalizacao {

    private Normalizacao() {
    }

    public static String email(String valor) {
        return valor == null ? null : unicode(valor).strip().toLowerCase(Locale.ROOT);
    }

    public static String nome(String valor) {
        return valor == null ? null : unicode(valor).strip().replaceAll("\\s+", " ");
    }

    public static String cpf(String valor) {
        return valor == null ? null : valor.replaceAll("[\\s.\\-]", "");
    }

    public static String telefone(String valor) {
        return valor == null ? null : valor.replaceAll("[\\s().\\-]", "");
    }

    public static String crm(String valor) {
        return valor == null ? null : valor.replaceAll("[\\s.\\-]", "");
    }

    public static String uf(String valor) {
        return valor == null ? null : valor.strip().toUpperCase(Locale.ROOT);
    }

    public static String token(String valor) {
        return valor == null ? null : valor.strip();
    }

    private static String unicode(String valor) {
        return Normalizer.normalize(valor, Normalizer.Form.NFKC);
    }
}
