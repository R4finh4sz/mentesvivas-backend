package com.mentesvivas.backend.entity.usuario;

import com.mentesvivas.backend.util.Normalizacao;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "usuarios")
@Getter
@Setter
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String nomeCompleto;

    @Column(length = 11)
    private String cpf;

    private LocalDate dataNascimento;

    @Column(length = 11)
    private String telefone;

    @Column(nullable = false, length = 254)
    private String email;

    @Column(nullable = false)
    private String senhaHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Role role;

    @Column(nullable = false)
    private boolean emailConfirmado;

    @Column(length = 10)
    private String crm;

    @Column(length = 2)
    private String crmUf;

    @Column(nullable = false)
    private boolean crmVerificado;

    private Instant dadosSaudeConsentidoEm;

    @Column(nullable = false)
    private Instant criadoEm;

    @Column(nullable = false)
    private Instant atualizadoEm;

    public void setEmail(String email) {
        this.email = Normalizacao.email(email);
    }

    @PrePersist
    void prePersist() {
        criadoEm = Instant.now();
        atualizadoEm = criadoEm;
    }

    @PreUpdate
    void preUpdate() {
        atualizadoEm = Instant.now();
    }
}
