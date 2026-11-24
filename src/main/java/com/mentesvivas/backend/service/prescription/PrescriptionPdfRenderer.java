package com.mentesvivas.backend.service.prescription;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Image;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.mentesvivas.backend.entity.prescription.DocumentKind;
import com.mentesvivas.backend.entity.prescription.PrescriptionItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class PrescriptionPdfRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale.ROOT)
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
    private static final Font BOLD = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 11);
    private static final Font SMALL = FontFactory.getFont(FontFactory.HELVETICA, 9);

    private final QrCodeImage qrCode;

    public byte[] render(PrescriptionPdfData data) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph(title(data.kind()), TITLE));
            if (data.simulated()) {
                document.add(new Paragraph("AMBIENTE DE TESTE - assinatura simulada, sem validade juridica.", BOLD));
            }
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Paciente: " + data.patientName(), BODY));
            document.add(new Paragraph("Emitido por: %s - %s".formatted(data.doctorName(), data.doctorRegistry()), BODY));
            document.add(new Paragraph("Data de emissao: " + DATE.format(data.issuedAt()), BODY));
            document.add(new Paragraph("Validade: ate " + DATE.format(data.validUntil()), BODY));
            document.add(new Paragraph(" "));
            addItems(document, data);
            if (data.orientations() != null) {
                document.add(new Paragraph("Orientacoes", BOLD));
                document.add(new Paragraph(data.orientations(), BODY));
                document.add(new Paragraph(" "));
            }
            addVerification(document, data);
            document.close();
        } catch (DocumentException | java.io.IOException ex) {
            throw new IllegalStateException("Nao foi possivel gerar o PDF.", ex);
        }
        return output.toByteArray();
    }

    private void addItems(Document document, PrescriptionPdfData data) throws DocumentException {
        int number = 1;
        for (PrescriptionItem item : data.items()) {
            document.add(new Paragraph("%d. %s - %s".formatted(number++, item.getMedicationName(),
                    nullToEmpty(item.getPresentation())), BOLD));
            document.add(new Paragraph("Dose: %s | Via: %s | Frequencia: %s | Duracao: %s | Quantidade: %s".formatted(
                    nullToEmpty(item.getDose()), nullToEmpty(item.getRoute()), nullToEmpty(item.getFrequency()),
                    nullToEmpty(item.getDuration()), nullToEmpty(item.getQuantity())), BODY));
            if (item.getInstructions() != null) {
                document.add(new Paragraph("Uso: " + item.getInstructions(), BODY));
            }
            document.add(new Paragraph(" "));
        }
    }

    private void addVerification(Document document, PrescriptionPdfData data)
            throws DocumentException, java.io.IOException {
        Image image = Image.getInstance(qrCode.png(data.verificationUrl()));
        image.scaleToFit(110, 110);
        image.setAlignment(Element.ALIGN_LEFT);
        document.add(image);
        document.add(new Paragraph("Verifique a autenticidade: " + data.verificationUrl(), SMALL));
        document.add(new Paragraph("Assinatura digital: " + data.signatureReference(), SMALL));
    }

    private String title(DocumentKind kind) {
        return switch (kind) {
            case PRESCRIPTION -> "Receita medica";
            case EXAM_GUIDE -> "Guia de exames";
            case CHECKLIST -> "Checklist de cuidado";
        };
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
