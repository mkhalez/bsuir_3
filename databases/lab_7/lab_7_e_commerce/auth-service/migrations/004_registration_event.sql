CREATE TABLE IF NOT EXISTS registration_event (
    id UUID PRIMARY KEY,
    user_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    payload TEXT NOT NULL,
    status TEXT NOT NULL DEFAULT 'CREATED',
    attempt_count INT NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    sending_started_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_reg_status CHECK (status IN ('CREATED','PRE_SENDING','SENDING','SUCCESS','FAIL','FAIL_TO_SEND'))
);
