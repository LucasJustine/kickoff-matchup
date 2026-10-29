package fr.kickoffmatchup.auth_server.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;


@Entity
@Table(name = "refresh_tokens", indexes = {
        @Index(columnList = "family_id"),
        @Index(columnList = "user_id")})
@Getter
@Setter
@Builder
@NoArgsConstructor
public class RefreshToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "family_id", nullable = false, length = 36)
    private String familyId;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "rotated", nullable = false)
    private boolean rotated;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public RefreshToken(Long id, String tokenHash, User user, String familyId,
                        Instant expiresAt, Instant revokedAt, boolean rotated,
                        LocalDateTime createdAt) {
        this.id = id;
        this.tokenHash = tokenHash;
        this.user = user;
        this.familyId = familyId;
        this.expiresAt = expiresAt;
        this.revokedAt = revokedAt;
        this.rotated = rotated;
        this.createdAt = createdAt;
    }
}
