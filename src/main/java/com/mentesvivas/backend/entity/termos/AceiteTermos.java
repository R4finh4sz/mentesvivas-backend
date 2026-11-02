package com.mentesvivas.backend.entity.termos;

import com.mentesvivas.backend.entity.usuario.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "aceites_termos")
@Immutable
@Getter
@NoArgsConstructor
public class AceiteTermos {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private Integer versao;

    @Column(nullable = false)
    private Instant aceitoEm;

    public AceiteTermos(Usuario usuario, Integer versao, Instant aceitoEm) {
        this.usuario = usuario;
        this.versao = versao;
        this.aceitoEm = aceitoEm;
    }
}
