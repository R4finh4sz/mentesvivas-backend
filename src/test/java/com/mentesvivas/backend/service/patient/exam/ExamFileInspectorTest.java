package com.mentesvivas.backend.service.patient.exam;

import com.mentesvivas.backend.entity.patient.ExamFileType;
import com.mentesvivas.backend.exception.patient.ImpossibleValueException;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExamFileInspectorTest {

    private final ExamFileInspector inspector = new ExamFileInspector();

    @Test
    void recognizesPdf() {
        var detected = inspector.inspect("%PDF-1.7 conteudo".getBytes(StandardCharsets.US_ASCII));

        assertThat(detected.type()).isEqualTo(ExamFileType.PDF);
        assertThat(detected.contentType()).isEqualTo("application/pdf");
    }

    @Test
    void recognizesJpeg() {
        var detected = inspector.inspect(new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0});

        assertThat(detected.type()).isEqualTo(ExamFileType.IMAGE);
        assertThat(detected.contentType()).isEqualTo("image/jpeg");
    }

    @Test
    void recognizesPng() {
        var detected = inspector.inspect(new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0});

        assertThat(detected.type()).isEqualTo(ExamFileType.IMAGE);
        assertThat(detected.contentType()).isEqualTo("image/png");
    }

    @Test
    void rejectsOtherContent() {
        assertThatThrownBy(() -> inspector.inspect("<html></html>".getBytes(StandardCharsets.US_ASCII)))
                .isInstanceOf(ImpossibleValueException.class);
    }

    @Test
    void rejectsContentShorterThanTheSignature() {
        assertThatThrownBy(() -> inspector.inspect(new byte[]{0x25, 0x50}))
                .isInstanceOf(ImpossibleValueException.class);
    }
}
