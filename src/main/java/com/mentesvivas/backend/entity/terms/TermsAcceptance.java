package com.mentesvivas.backend.entity.terms;

import com.mentesvivas.backend.entity.user.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "terms_acceptances")
@Immutable
@Getter
@NoArgsConstructor
public class TermsAcceptance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private Integer version;

    @Column(nullable = false)
    private Instant acceptedAt;

    public TermsAcceptance(User user, Integer version, Instant acceptedAt) {
        this.user = user;
        this.version = version;
        this.acceptedAt = acceptedAt;
    }
}
