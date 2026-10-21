CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(active);
CREATE INDEX IF NOT EXISTS idx_user_roles_user ON user_roles(user_id);
CREATE INDEX IF NOT EXISTS idx_user_roles_role ON user_roles(role_id);
CREATE INDEX IF NOT EXISTS idx_reg_event_status_next ON registration_event(status, next_attempt_at);
CREATE INDEX IF NOT EXISTS idx_reg_event_user ON registration_event(user_id);
CREATE INDEX IF NOT EXISTS idx_auth_audit_user_time ON auth_audit_log(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_auth_audit_action_time ON auth_audit_log(action, created_at DESC);
