package com.mentesvivas.backend.entity.termos;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "termos_versoes")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TermosVersao {

    @Id
    private Integer versao;

    @Column(nullable = false, length = 200)
    private String titulo;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String conteudo;

    @Column(nullable = false)
    private UUID publicadoPor;

    @Column(nullable = false)
    private Instant publicadoEm;
}
