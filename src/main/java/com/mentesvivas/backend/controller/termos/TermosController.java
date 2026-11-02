package com.mentesvivas.backend.controller.termos;

import com.mentesvivas.backend.dto.termos.AceitarTermosRequest;
import com.mentesvivas.backend.dto.termos.AceiteTermosResponse;
import com.mentesvivas.backend.dto.termos.PublicarTermosRequest;
import com.mentesvivas.backend.dto.termos.TermosResponse;
import com.mentesvivas.backend.service.termos.AceiteTermosService;
import com.mentesvivas.backend.service.termos.TermosService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/terms")
@RequiredArgsConstructor
public class TermosController {

    private final TermosService termosService;
    private final AceiteTermosService aceiteTermosService;

    @GetMapping
    public TermosResponse atual() {
        return termosService.atual();
    }

    @GetMapping("/history")
    public List<TermosResponse> historico() {
        return termosService.historico();
    }

    @GetMapping("/accepted")
    public List<AceiteTermosResponse> aceites() {
        return aceiteTermosService.aceitesDoUsuarioAtual();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TermosResponse publicar(@Valid @RequestBody PublicarTermosRequest body) {
        return termosService.publicar(body);
    }

    @PostMapping("/accept")
    public AceiteTermosResponse aceitar(@Valid @RequestBody AceitarTermosRequest body) {
        return aceiteTermosService.aceitar(body);
    }
}
