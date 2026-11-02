package com.mentesvivas.backend.config;

import com.mentesvivas.backend.entity.usuario.Role;
import com.mentesvivas.backend.entity.usuario.Usuario;
import com.mentesvivas.backend.exception.config.ConfiguracaoInvalidaException;
import com.mentesvivas.backend.repository.usuario.UsuarioRepository;
import com.mentesvivas.backend.util.Normalizacao;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;

@Component
public class AdminBootstrap implements CommandLineRunner {

    private static final int SENHA_TAMANHO_MINIMO = 12;
    private static final int SENHA_BYTES_MAXIMO = 72;

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final String email;
    private final String senha;

    public AdminBootstrap(UsuarioRepository usuarioRepository,
                          PasswordEncoder passwordEncoder,
                          @Value("${app.admin.email:}") String email,
                          @Value("${app.admin.senha:}") String senha) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.email = email;
        this.senha = senha;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (email == null || email.isBlank()) {
            return;
        }
        String emailNormalizado = Normalizacao.email(email);
        if (usuarioRepository.existsByEmail(emailNormalizado)) {
            return;
        }
        validarSenha();

        Usuario admin = new Usuario();
        admin.setNomeCompleto("Administrador");
        admin.setEmail(emailNormalizado);
        admin.setSenhaHash(passwordEncoder.encode(senha));
        admin.setRole(Role.ADMINISTRADOR);
        admin.setEmailConfirmado(true);
        usuarioRepository.save(admin);
    }

    private void validarSenha() {
        if (senha == null || senha.length() < SENHA_TAMANHO_MINIMO
                || senha.getBytes(StandardCharsets.UTF_8).length > SENHA_BYTES_MAXIMO) {
            throw new ConfiguracaoInvalidaException(
                    "BOOTSTRAP_ADMIN_PASSWORD deve ter no minimo 12 caracteres e no maximo 72 bytes.");
        }
    }
}
