CREATE TABLE users (
    id              BIGINT          NOT NULL AUTO_INCREMENT,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(100)    DEFAULT NULL,
    phone           VARCHAR(20)     DEFAULT NULL,
    password_hash   VARCHAR(72)     NOT NULL  COMMENT 'BCrypt hash',
    avatar_url      VARCHAR(512)    DEFAULT NULL,
    gender          TINYINT         NOT NULL  DEFAULT 0  COMMENT '0=UNSPECIFIED 1=MALE 2=FEMALE',
    birthday        DATE            DEFAULT NULL,
    real_name       VARCHAR(50)     DEFAULT NULL  COMMENT '实名认证姓名',
    status          TINYINT         NOT NULL  DEFAULT 0  COMMENT '0=UNVERIFIED 1=ACTIVE 2=SUSPENDED 3=DELETED',
    role            TINYINT         NOT NULL  DEFAULT 0  COMMENT '0=USER 1=ADMIN 2=CONTENT_OPERATOR',
    email_verified  TINYINT(1)      NOT NULL  DEFAULT 0,
    phone_verified  TINYINT(1)      NOT NULL  DEFAULT 0,
    last_login_at   DATETIME        DEFAULT NULL,
    created_at      DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    updated_at      DATETIME        NOT NULL  DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT pk_users          PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username),
    CONSTRAINT uq_users_email    UNIQUE (email),
    CONSTRAINT uq_users_phone    UNIQUE (phone)
);

CREATE INDEX idx_users_email  ON users (email);
CREATE INDEX idx_users_phone  ON users (phone);
CREATE INDEX idx_users_status ON users (status);

CREATE TABLE refresh_tokens (
    id          BIGINT       NOT NULL AUTO_INCREMENT,
    user_id     BIGINT       NOT NULL,
    token_hash  VARCHAR(64)  NOT NULL  COMMENT 'SHA-256 hex，不存原始值',
    device_info VARCHAR(255) DEFAULT NULL,
    issued_at   DATETIME     NOT NULL  DEFAULT CURRENT_TIMESTAMP,
    expires_at  DATETIME     NOT NULL,
    revoked     TINYINT(1)   NOT NULL  DEFAULT 0,
    revoked_at  DATETIME     DEFAULT NULL,

    CONSTRAINT pk_refresh_tokens PRIMARY KEY (id),
    CONSTRAINT uq_refresh_token  UNIQUE (token_hash),
    CONSTRAINT fk_rt_user_id     FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_rt_user_id    ON refresh_tokens (user_id);
CREATE INDEX idx_rt_token_hash ON refresh_tokens (token_hash);
CREATE INDEX idx_rt_expires_at ON refresh_tokens (expires_at);

CREATE TABLE password_reset_tokens (
    id         BIGINT      NOT NULL AUTO_INCREMENT,
    user_id    BIGINT      NOT NULL,
    token_hash VARCHAR(64) NOT NULL  COMMENT 'SHA-256 of OTP',
    channel    VARCHAR(10) NOT NULL  COMMENT 'EMAIL or SMS',
    expires_at DATETIME    NOT NULL,
    used       TINYINT(1)  NOT NULL  DEFAULT 0,

    CONSTRAINT pk_prt            PRIMARY KEY (id),
    CONSTRAINT uq_prt_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_prt_user_id    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_user_id ON password_reset_tokens (user_id);