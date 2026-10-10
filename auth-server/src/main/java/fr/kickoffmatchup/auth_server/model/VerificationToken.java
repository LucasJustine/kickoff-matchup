package fr.kickoffmatchup.auth_server.model;

import fr.kickoffmatchup.auth_server.enums.TokenType;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/** Token à usage unique (vérification d'email, reset de mot de passe). Seul le hash est stocké. */
@Entity
@Table(name = "verification_tokens", indexes = @Index(columnList = "user_id,type"))
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class VerificationToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private TokenType type;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;
}
