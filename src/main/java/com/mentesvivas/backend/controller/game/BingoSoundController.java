package com.mentesvivas.backend.controller.game;

import com.mentesvivas.backend.dto.game.BingoSoundResponse;
import com.mentesvivas.backend.dto.game.SaveBingoSoundRequest;
import com.mentesvivas.backend.service.game.BingoSoundService;
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

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/bingo-sounds")
@RequiredArgsConstructor
public class BingoSoundController {

    private final BingoSoundService service;

    @GetMapping
    public List<BingoSoundResponse> list() {
        return service.list();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public BingoSoundResponse create(@Valid @RequestBody SaveBingoSoundRequest body) {
        return service.create(body);
    }

    @PutMapping("/{id}")
    public BingoSoundResponse update(@PathVariable UUID id, @Valid @RequestBody SaveBingoSoundRequest body) {
        return service.update(id, body);
    }
}
