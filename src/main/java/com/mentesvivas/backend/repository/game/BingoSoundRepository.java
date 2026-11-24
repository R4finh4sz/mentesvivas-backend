package com.mentesvivas.backend.repository.game;

import com.mentesvivas.backend.entity.game.BingoSound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface BingoSoundRepository extends JpaRepository<BingoSound, UUID> {

    List<BingoSound> findAllByOrderByInstrumentAsc();

    List<BingoSound> findByActiveTrueOrderByInstrumentAsc();

    boolean existsBySearchKey(String searchKey);

    boolean existsBySearchKeyAndIdNot(String searchKey, UUID id);
}
