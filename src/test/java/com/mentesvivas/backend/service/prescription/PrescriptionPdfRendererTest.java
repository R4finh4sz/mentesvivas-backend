package com.mentesvivas.backend.service.prescription;

import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PrescriptionPdfRendererTest {

    private final PrescriptionPdfRenderer renderer = new PrescriptionPdfRenderer(new QrCodeImage());

    private PrescriptionPdfData data(boolean simulated) {
        PrescriptionItem item = new PrescriptionItem();
        item.setMedicationName("Losartana");
        item.setPresentation("50 mg");
        item.setDose("1 comprimido");
        item.setRoute("Oral");
        item.setFrequency("12/12h");
        item.setDuration("30 dias");
        item.setQuantity("60");
        item.setInstructions("Tomar apos o cafe");
        return new PrescriptionPdfData(DocumentKind.PRESCRIPTION, "Maria Souza", "Dr. Paulo", "CRM 123456/SP",
                Instant.parse("2026-11-23T12:00:00Z"), Instant.parse("2026-12-23T12:00:00Z"), "Retorno em 30 dias",
                List.of(item), "https://verifica.mentesvivas.test/receitas/abc", "SIMULADA-123", simulated);
    }

    @Test
    void producesAPdfDocument() {
        byte[] pdf = renderer.render(data(false));

        assertThat(new String(pdf, 0, 5, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF-");
        assertThat(pdf.length).isGreaterThan(1000);
    }

    @Test
    void theVerificationUrlChangesTheEmbeddedQrCode() {
        PrescriptionPdfData base = data(false);
        PrescriptionPdfData other = new PrescriptionPdfData(base.kind(), base.patientName(), base.doctorName(),
                base.doctorRegistry(), base.issuedAt(), base.validUntil(), base.orientations(), base.items(),
                "https://verifica.mentesvivas.test/receitas/outro-codigo", base.signatureReference(), false);

        assertThat(renderer.render(base)).isNotEqualTo(renderer.render(other));
    }

    @Test
    void qrCodeImageIsAValidPng() {
        byte[] png = new QrCodeImage().png("https://verifica.mentesvivas.test/receitas/abc");

        assertThat(png[1]).isEqualTo((byte) 'P');
        assertThat(png[2]).isEqualTo((byte) 'N');
        assertThat(png[3]).isEqualTo((byte) 'G');
    }

    @Test
    void differentDocumentsProduceDifferentFiles() {
        assertThat(renderer.render(data(true))).isNotEqualTo(renderer.render(data(false)));
    }
}
