package com.mentesvivas.backend.dto.user;

final class PhotoUrl {

    static final String REGEX = "^(https://\\S+)?$";
    static final String MESSAGE = "informe o link da foto comecando com https://";

    private PhotoUrl() {
    }

    static String normalize(String value) {
        return value == null ? null : value.strip();
    }
}
