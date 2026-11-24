package com.mentesvivas.backend.entity.prescription;

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
@Table(name = "drug_catalog")
@Getter
@Setter
@NoArgsConstructor
public class DrugCatalogEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String name;

    @Column(nullable = false, length = 150)
    private String searchKey;

    @Column(nullable = false, length = 150)
    private String activeIngredient;

    @Column(nullable = false, length = 150)
    private String ingredientKey;

    @Column(length = 100)
    private String therapeuticClass;

    @Column(nullable = false)
    private boolean controlled;

    @Column(nullable = false)
    private boolean active = true;

    @Column(nullable = false)
    private Instant createdAt;
}
