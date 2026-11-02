package com.mentesvivas.backend.controller.auth;

import com.mentesvivas.backend.dto.usuario.RegistroMedicoRequest;
import com.mentesvivas.backend.dto.usuario.RegistroRequest;
import com.mentesvivas.backend.dto.usuario.UsuarioResponse;
import com.mentesvivas.backend.service.usuario.RegistroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/register")
@RequiredArgsConstructor
public class RegistroController {

    private final RegistroService registroService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrar(@Valid @RequestBody RegistroRequest body) {
        return registroService.registrar(body);
    }

    @PostMapping("/medico")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse registrarMedico(@Valid @RequestBody RegistroMedicoRequest body) {
        return registroService.registrarMedico(body);
    }
}
