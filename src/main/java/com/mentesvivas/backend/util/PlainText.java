package com.mentesvivas.backend.util;

import java.text.Normalizer;
import java.util.Locale;

public final class PlainText {

    private PlainText() {
    }

    public static String key(String value) {
        if (value == null) {
            return "";
        }
        return Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .strip();
    }
}
