package com.mentesvivas.backend.controller.game;

import com.mentesvivas.backend.dto.game.AnswerBingoRequest;
import com.mentesvivas.backend.dto.game.BingoAnswerResponse;
import com.mentesvivas.backend.dto.game.BingoRoundResponse;
import com.mentesvivas.backend.dto.game.BingoSettingsResponse;
import com.mentesvivas.backend.dto.game.StartBingoResponse;
import com.mentesvivas.backend.dto.game.UpdateBingoSettingsRequest;
import com.mentesvivas.backend.service.game.BingoGameService;
import com.mentesvivas.backend.service.game.BingoSettingsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/games/musical-bingo")
@RequiredArgsConstructor
public class BingoGameController {

    private final BingoGameService game;
    private final BingoSettingsService settings;

    @GetMapping("/settings")
    public BingoSettingsResponse settings(@PathVariable UUID patientId) {
        return settings.get(patientId);
    }

    @PutMapping("/settings")
    public BingoSettingsResponse changeSettings(@PathVariable UUID patientId,
                                                @Valid @RequestBody UpdateBingoSettingsRequest body) {
        return settings.changeNarration(patientId, body.narrationEnabled());
    }

    @PostMapping("/sessions")
    @ResponseStatus(HttpStatus.CREATED)
    public StartBingoResponse start(@PathVariable UUID patientId) {
        return game.start(patientId);
    }

    @GetMapping("/sessions/{sessionId}/round")
    public BingoRoundResponse current(@PathVariable UUID patientId, @PathVariable UUID sessionId) {
        return game.current(patientId, sessionId);
    }

    @PostMapping("/sessions/{sessionId}/answers")
    public BingoAnswerResponse answer(@PathVariable UUID patientId, @PathVariable UUID sessionId,
                                      @Valid @RequestBody AnswerBingoRequest body) {
        return game.answer(patientId, sessionId, body);
    }
}
