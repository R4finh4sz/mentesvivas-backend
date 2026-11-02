package com.mentesvivas.backend.service.usuario;

import com.mentesvivas.backend.dto.usuario.RegistroMedicoRequest;
import com.mentesvivas.backend.dto.usuario.RegistroRequest;
import com.mentesvivas.backend.dto.usuario.UsuarioResponse;
import com.mentesvivas.backend.entity.usuario.Role;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.mapper.usuario.UsuarioMapper;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import com.mentesvivas.backend.service.termos.AceiteTermosService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class RegistroService {

    private final UsuarioRepository usuarioRepository;
    private final UsuarioUnicidadeValidator unicidadeValidator;
    private final PasswordEncoder passwordEncoder;
    private final AceiteTermosService aceiteTermosService;

    @Transactional
    public UsuarioResponse registrar(RegistroRequest request) {
        aceiteTermosService.validarVersaoAtual(request.versaoTermos());
        Usuario usuario = novoUsuario(request.nomeCompleto(), request.cpf(), request.telefone(),
                request.email(), request.senha(), request.role());
        usuario.setDataNascimento(request.dataNascimento());
        usuario.setDadosSaudeConsentidoEm(Instant.now());
        return salvarComAceite(usuario, request.versaoTermos());
    }

    @Transactional
    public UsuarioResponse registrarMedico(RegistroMedicoRequest request) {
        aceiteTermosService.validarVersaoAtual(request.versaoTermos());
        Usuario usuario = novoUsuario(request.nomeCompleto(), request.cpf(), request.telefone(),
                request.email(), request.senha(), Role.MEDICO);
        usuario.setCrm(request.crm());
        usuario.setCrmUf(request.crmUf());
        usuario.setCrmVerificado(false);
        return salvarComAceite(usuario, request.versaoTermos());
    }

    private UsuarioResponse salvarComAceite(Usuario usuario, Integer versaoTermos) {
        Usuario salvo = usuarioRepository.save(usuario);
        aceiteTermosService.registrar(salvo, versaoTermos);
        return UsuarioMapper.toResponse(salvo);
    }

    private Usuario novoUsuario(String nome, String cpf, String telefone, String email, String senha, Role role) {
        unicidadeValidator.validar(email, cpf);

        Usuario usuario = new Usuario();
        usuario.setNomeCompleto(nome);
        usuario.setCpf(cpf);
        usuario.setTelefone(telefone);
        usuario.setEmail(email);
        usuario.setSenhaHash(passwordEncoder.encode(senha));
        usuario.setRole(role);
        usuario.setEmailConfirmado(false);
        return usuario;
    }
}
