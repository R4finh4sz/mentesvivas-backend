package com.mentesvivas.backend.entity.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bingo_settings")
@Getter
@Setter
@NoArgsConstructor
public class BingoSettings {

    @Id
    private UUID patientId;

    @Column(nullable = false)
    private boolean narrationEnabled;

    @Column(nullable = false)
    private UUID updatedBy;

    @Column(nullable = false)
    private Instant updatedAt;
}
