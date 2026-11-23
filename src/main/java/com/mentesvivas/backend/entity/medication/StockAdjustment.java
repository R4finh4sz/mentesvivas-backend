package com.mentesvivas.backend.entity.medication;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "stock_adjustments")
@Getter
@NoArgsConstructor
public class StockAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private UUID medicationId;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal previousQuantity;

    @Column(nullable = false, precision = 8, scale = 2)
    private BigDecimal newQuantity;

    @Column(nullable = false, length = 300)
    private String justification;

    @Column(nullable = false)
    private UUID adjustedBy;

    @Column(nullable = false)
    private Instant adjustedAt;

    public StockAdjustment(UUID medicationId, BigDecimal previousQuantity, BigDecimal newQuantity,
                           String justification, UUID adjustedBy, Instant adjustedAt) {
        this.medicationId = medicationId;
        this.previousQuantity = previousQuantity;
        this.newQuantity = newQuantity;
        this.justification = justification;
        this.adjustedBy = adjustedBy;
        this.adjustedAt = adjustedAt;
    }
}
