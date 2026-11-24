package com.mentesvivas.backend.service.game;

import com.mentesvivas.backend.dto.game.GamePerformanceReport;
import com.mentesvivas.backend.entity.audit.PatientDataArea;
import com.mentesvivas.backend.entity.user.User;
import com.mentesvivas.backend.repository.game.BingoRoundRepository;
import com.mentesvivas.backend.repository.game.RecognitionSessionRepository;
import com.mentesvivas.backend.service.audit.AccessLog;
import com.mentesvivas.backend.service.clinical.DoctorPatientAccess;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class GamePerformanceService {

    private final DoctorPatientAccess access;
    private final AccessLog accessLog;
    private final RecognitionSessionRepository recognition;
    private final BingoRoundRepository bingo;

    @Transactional
    public GamePerformanceReport report(UUID patientId) {
        User doctor = access.requireDoctorOf(patientId);
        accessLog.record(doctor.getId(), patientId, PatientDataArea.GAME_PERFORMANCE);
        return new GamePerformanceReport(recognition.performance(patientId), bingo.performance(patientId),
                "O desempenho nos jogos não é um diagnóstico e não substitui a avaliação neuropsicológica.");
    }
}
