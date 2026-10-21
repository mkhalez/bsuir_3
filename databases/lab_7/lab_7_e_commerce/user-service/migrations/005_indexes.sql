CREATE INDEX IF NOT EXISTS idx_profiles_email ON profiles(email);
CREATE INDEX IF NOT EXISTS idx_profiles_auth_id ON profiles(auth_user_id);
CREATE INDEX IF NOT EXISTS idx_profiles_active ON profiles(active);
CREATE INDEX IF NOT EXISTS idx_profiles_last_name ON profiles(last_name, first_name);
CREATE INDEX IF NOT EXISTS idx_addresses_profile ON addresses(profile_id);
CREATE INDEX IF NOT EXISTS idx_user_audit_profile_time ON user_audit_log(profile_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_reg_outbox_sent_next ON registration_outbox(sent, next_attempt_at);
