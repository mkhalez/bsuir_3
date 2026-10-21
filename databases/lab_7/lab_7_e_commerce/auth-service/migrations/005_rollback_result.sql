CREATE TABLE IF NOT EXISTS registration_result (
    id UUID PRIMARY KEY,
    status TEXT NOT NULL,
    message TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
