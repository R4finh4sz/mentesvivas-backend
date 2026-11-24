package com.mentesvivas.backend.entity.patient;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "cid10_catalog")
@Getter
@NoArgsConstructor
public class Cid10Entry {

    @Id
    @Column(length = 5)
    private String code;

    @Column(nullable = false, length = 400)
    private String description;

    public Cid10Entry(String code, String description) {
        this.code = code;
        this.description = description;
    }
}
