package com.mentesvivas.backend.entity.terms;

import com.mentesvivas.backend.entity.user.Role;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Immutable;

import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "terms_versions")
@Immutable
@Getter
@NoArgsConstructor
public class TermsVersion {

    @Id
    private Integer version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private TermsType type = TermsType.TERMS_OF_USE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TermsAudience audience = TermsAudience.ALL;

    @Convert(converter = RoleSetConverter.class)
    @Column(length = 100)
    private Set<Role> audienceRoles = EnumSet.noneOf(Role.class);

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private UUID publishedBy;

    @Column(nullable = false)
    private Instant publishedAt;

    public TermsVersion(Integer version, String title, String content, UUID publishedBy, Instant publishedAt) {
        this(version, TermsType.TERMS_OF_USE, TermsAudience.ALL, Set.of(), title, content, publishedBy, publishedAt);
    }

    public TermsVersion(Integer version, TermsType type, TermsAudience audience, Set<Role> audienceRoles,
                        String title, String content, UUID publishedBy, Instant publishedAt) {
        this.version = version;
        this.type = type;
        this.audience = audience;
        this.audienceRoles = audienceRoles == null || audienceRoles.isEmpty()
                ? EnumSet.noneOf(Role.class) : EnumSet.copyOf(audienceRoles);
        this.title = title;
        this.content = content;
        this.publishedBy = publishedBy;
        this.publishedAt = publishedAt;
    }

    public boolean appliesTo(Role role) {
        return audience == TermsAudience.ALL || audienceRoles.contains(role);
    }
}
