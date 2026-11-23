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

import java.util.UUID;

@Entity
@Table(name = "prescription_items")
@Getter
@Setter
@NoArgsConstructor
public class PrescriptionItem {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID prescriptionId;

    @Column(nullable = false)
    private int position;

    @Column(length = 150)
    private String medicationName;

    @Column(length = 100)
    private String presentation;

    @Column(length = 100)
    private String dose;

    @Column(length = 60)
    private String route;

    @Column(length = 100)
    private String frequency;

    @Column(length = 100)
    private String duration;

    @Column(length = 60)
    private String quantity;

    @Column(length = 500)
    private String instructions;

    @Column(nullable = false)
    private boolean continuousUse;

    @Column(length = 300)
    private String allergyOverrideJustification;
}
