package com.mentesvivas.backend.service.clinical.timeline;

import com.lowagie.text.Document;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfWriter;
import com.mentesvivas.backend.dto.clinical.TimelineResponse;
import com.mentesvivas.backend.dto.patient.VitalSignResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;

@Component
public class TimelinePdfRenderer {

    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT)
            .withZone(ZoneId.of("America/Sao_Paulo"));
    private static final Font TITLE = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
    private static final Font HEADING = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
    private static final Font BODY = FontFactory.getFont(FontFactory.HELVETICA, 10);

    public byte[] render(String patient, String doctor, TimelineResponse timeline) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        Document document = new Document();
        try {
            PdfWriter.getInstance(document, output);
            document.open();
            document.add(new Paragraph("Linha do tempo clínica", TITLE));
            document.add(new Paragraph("Paciente: " + patient, BODY));
            document.add(new Paragraph("Exportado por: " + doctor, BODY));
            document.add(new Paragraph("Período: %s a %s (horário de São Paulo)"
                    .formatted(date(timeline.from()), date(timeline.to())), BODY));
            section(document, "Aferições", timeline.vitalSigns(), TimelinePdfRenderer::vital);
            section(document, "Humor", timeline.moods(), row ->
                    date(row.occurredAt()) + " - " + row.mood()
                            + note(row.notes(), row.checkAgainstOriginal()));
            section(document, "Sono", timeline.sleeps(), row ->
                    "%s - %s h | %s%s".formatted(date(row.occurredAt()), row.sleepHours(), row.sleepQuality(),
                            note(row.notes(), row.checkAgainstOriginal())));
            section(document, "Ocorrências", timeline.occurrences(), row ->
                    "%s - %s | %s | Ida ao hospital: %s%s".formatted(date(row.occurredAt()), row.type(),
                            row.severity(), row.needsHospital() ? "sim" : "não", note(row.description(), false)));
            section(document, "Medicação", timeline.medicationDoses(), row ->
                    "%s - %s | %s | Registro: %s".formatted(date(row.scheduledFor()), row.medication(),
                            row.status(), date(row.recordedAt())));
            section(document, "Peso", timeline.weights(), row ->
                    "%s - %s kg".formatted(date(row.measuredAt()), row.weightKg()));
        } finally {
            document.close();
        }
        return output.toByteArray();
    }

    private <T> void section(Document document, String name, List<T> rows, Function<T, String> text) {
        Paragraph heading = new Paragraph(name, HEADING);
        heading.setSpacingBefore(12);
        heading.setKeepTogether(true);
        document.add(heading);
        if (rows.isEmpty()) {
            document.add(new Paragraph("Sem registros no período.", BODY));
        }
        for (T row : rows) {
            document.add(new Paragraph(text.apply(row), BODY));
        }
    }

    private static String vital(VitalSignResponse row) {
        List<String> values = new ArrayList<>();
        if (row.systolic() != null) {
            values.add("PA %s/%s mmHg".formatted(row.systolic(), row.diastolic()));
        }
        if (row.heartRate() != null) {
            values.add("FC " + row.heartRate() + " bpm");
        }
        if (row.glucose() != null) {
            values.add("Glicemia %s mg/dL (%s)".formatted(row.glucose(), row.glucoseType()));
        }
        if (row.temperature() != null) {
            values.add("Temperatura " + row.temperature() + " °C");
        }
        if (row.oxygenSaturation() != null) {
            values.add("SpO2 " + row.oxygenSaturation() + "%");
        }
        return date(row.measuredAt()) + " - " + String.join(" | ", values)
                + note(row.notes(), row.checkAgainstOriginal());
    }

    private static String note(String value, boolean checkAgainstOriginal) {
        String note = value == null ? "" : " | " + value;
        return checkAgainstOriginal ? note + " | Conferir com o documento original." : note;
    }

    private static String date(Instant instant) {
        return DATE.format(instant);
    }
}
