package com.mentesvivas.backend.entity.game;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "bingo_sounds")
@Getter
@Setter
@NoArgsConstructor
public class BingoSound {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 60)
    private String instrument;

    @Column(nullable = false, unique = true, length = 60)
    private String searchKey;

    @Column(nullable = false, length = 500)
    private String audioUrl;

    @Column(nullable = false)
    private boolean active;

    @Column(nullable = false)
    private Instant createdAt;
}
