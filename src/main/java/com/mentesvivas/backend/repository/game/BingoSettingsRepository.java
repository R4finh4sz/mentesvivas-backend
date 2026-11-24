package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.BingoSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface BingoSettingsRepository extends JpaRepository<BingoSettings, UUID> {
}
