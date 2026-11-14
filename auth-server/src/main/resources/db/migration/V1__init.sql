CREATE TABLE users (
    id bigint NOT NULL AUTO_INCREMENT,
    email varchar(255) NOT NULL,
    password varchar(255) DEFAULT NULL,
    first_name varchar(255) NOT NULL,
    last_name varchar(255) NOT NULL,
    email_verified tinyint(1) NOT NULL DEFAULT 0,
    mfa_enabled tinyint(1) NOT NULL DEFAULT 0,
    created_at datetime(6) NOT NULL,
    modified_at datetime(6) NOT NULL,
    PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_users_email ON users (email);

CREATE TABLE refresh_tokens (
    id bigint NOT NULL AUTO_INCREMENT,
    token_hash varchar(64) NOT NULL,
    user_id bigint NOT NULL,
    family_id varchar(36) NOT NULL,
    expires_at datetime(6) NOT NULL,
    revoked_at datetime(6) DEFAULT NULL,
    rotated bit(1) NOT NULL,
    created_at datetime(6) NOT NULL,
    login_method enum ('PASSWORD', 'PROVIDER') DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_refresh_tokens_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_refresh_tokens_family_id ON refresh_tokens (family_id);
CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);

CREATE TABLE user_identities (
    id bigint NOT NULL AUTO_INCREMENT,
    user_id bigint NOT NULL,
    provider enum ('APPLE', 'GOOGLE') NOT NULL,
    provider_user_id varchar(255) NOT NULL,
    provider_email varchar(255) DEFAULT NULL,
    created_at datetime(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_user_identities_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_user_identities_provider_user ON user_identities (provider, provider_user_id);
CREATE INDEX idx_user_identities_user_id ON user_identities (user_id);

CREATE TABLE verification_tokens (
    id bigint NOT NULL AUTO_INCREMENT,
    token_hash varchar(64) NOT NULL,
    user_id bigint NOT NULL,
    type enum ('EMAIL_CHANGE', 'EMAIL_VERIFICATION', 'PASSWORD_RESET') NOT NULL,
    expires_at datetime(6) NOT NULL,
    used_at datetime(6) DEFAULT NULL,
    pending_password_hash varchar(100) DEFAULT NULL,
    pending_email varchar(254) DEFAULT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_verification_tokens_user FOREIGN KEY (user_id) REFERENCES users (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE UNIQUE INDEX uk_verification_tokens_token_hash ON verification_tokens (token_hash);
CREATE INDEX idx_verification_tokens_user_type ON verification_tokens (user_id, type);
