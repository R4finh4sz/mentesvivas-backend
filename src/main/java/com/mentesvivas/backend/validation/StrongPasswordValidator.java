package com.mentesvivas.backend.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isEmpty()) {
            return true;
        }
        boolean upper = false;
        boolean lower = false;
        boolean digit = false;
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            upper |= Character.isUpperCase(c);
            lower |= Character.isLowerCase(c);
            digit |= Character.isDigit(c);
        }
        return upper && lower && digit;
    }
}
