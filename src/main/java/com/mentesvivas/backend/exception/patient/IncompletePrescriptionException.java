package com.mentesvivas.backend.exception.patient;

import java.util.List;

public class IncompletePrescriptionException extends RuntimeException {

    private final List<String> missingFields;

    public IncompletePrescriptionException(List<String> missingFields) {
        super("Complete os campos obrigatorios antes de assinar.");
        this.missingFields = List.copyOf(missingFields);
    }

    public List<String> getMissingFields() {
        return missingFields;
    }
}
