CREATE TABLE IF NOT EXISTS blind_box_open_outbox (
    event_id VARCHAR(36) NOT NULL PRIMARY KEY,
    order_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    next_attempt_at DATETIME(6) NOT NULL,
    published_at DATETIME(6) NULL,
    KEY idx_open_outbox_pending (published_at, next_attempt_at, event_id)
) ENGINE=InnoDB;
