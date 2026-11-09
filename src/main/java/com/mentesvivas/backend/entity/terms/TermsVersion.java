package com.mentesvivas.backend.entity.terms;

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
@Table(name = "terms_versions")
@Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TermsVersion {

    @Id
    private Integer version;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private UUID publishedBy;

    @Column(nullable = false)
    private Instant publishedAt;
}
