package com.mentesvivas.backend.controller.game;

import com.mentesvivas.backend.dto.game.AnswerRecognitionRequest;
import com.mentesvivas.backend.dto.game.RecognitionAnswerResponse;
import com.mentesvivas.backend.dto.game.RecognitionQuestionResponse;
import com.mentesvivas.backend.service.game.RecognitionGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/care/patients/{patientId}/games/family-recognition/sessions")
@RequiredArgsConstructor
public class RecognitionGameController {

    private final RecognitionGameService game;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RecognitionQuestionResponse start(@PathVariable UUID patientId) {
        return game.start(patientId);
    }

    @GetMapping("/{sessionId}/question")
    public RecognitionQuestionResponse current(@PathVariable UUID patientId, @PathVariable UUID sessionId) {
        return game.current(patientId, sessionId);
    }

    @PostMapping("/{sessionId}/answers")
    public RecognitionAnswerResponse answer(@PathVariable UUID patientId, @PathVariable UUID sessionId,
                                            @Valid @RequestBody AnswerRecognitionRequest body) {
        return game.answer(patientId, sessionId, body);
    }
}
