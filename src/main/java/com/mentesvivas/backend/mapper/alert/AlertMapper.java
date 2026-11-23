package com.mentesvivas.backend.mapper.alert;

import com.mentesvivas.backend.dto.alert.AlertResponse;
import com.mentesvivas.backend.entity.alert.SosAlert;

public final class AlertMapper {

    public static final String NOTICE = "Este alerta nao substitui o servico de emergencia. Em caso de risco, ligue 192.";

    private AlertMapper() {
    }

    public static AlertResponse toResponse(SosAlert alert) {
        return new AlertResponse(alert.getId(), alert.getPatientId(), alert.getStatus(), alert.getCreatedAt(),
                alert.getSeenAt(), alert.getSeenBy(), alert.getAcknowledgedAt(), alert.getAcknowledgedBy(),
                alert.getResolvedAt(), alert.getResolvedBy(), NOTICE);
    }
}
