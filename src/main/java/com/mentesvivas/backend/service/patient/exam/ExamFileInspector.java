package com.mentesvivas.backend.service.patient.exam;

import com.mentesvivas.backend.entity.patient.ExamFileType;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import org.springframework.stereotype.Component;

@Component
public class ExamFileInspector {

    public record Detected(ExamFileType type, String contentType) {
    }

    public Detected inspect(byte[] content) {
        if (startsWith(content, 0x25, 0x50, 0x44, 0x46, 0x2D)) {
            return new Detected(ExamFileType.PDF, "application/pdf");
        }
        if (startsWith(content, 0xFF, 0xD8, 0xFF)) {
            return new Detected(ExamFileType.IMAGE, "image/jpeg");
        }
        if (startsWith(content, 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A)) {
            return new Detected(ExamFileType.IMAGE, "image/png");
        }
        throw new ImpossibleValueException("O arquivo deve ser um PDF, JPEG ou PNG.");
    }

    private boolean startsWith(byte[] content, int... signature) {
        if (content.length < signature.length) {
            return false;
        }
        for (int index = 0; index < signature.length; index++) {
            if ((content[index] & 0xFF) != signature[index]) {
                return false;
            }
        }
        return true;
    }
}
