package com.mentesvivas.backend.repository.prescription;

import com.mentesvivas.backend.entity.prescription.DrugCatalogEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DrugCatalogRepository extends JpaRepository<DrugCatalogEntry, UUID> {

    boolean existsBySearchKey(String searchKey);

    boolean existsBySearchKeyAndIdNot(String searchKey, UUID id);

    Optional<DrugCatalogEntry> findFirstByActiveTrueAndSearchKeyOrActiveTrueAndIngredientKey(String searchKey,
                                                                                              String ingredientKey);

    List<DrugCatalogEntry> findAllByOrderByNameAsc();
}
