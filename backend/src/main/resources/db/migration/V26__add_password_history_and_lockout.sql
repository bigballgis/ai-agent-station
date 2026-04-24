-- V26: Add password history table and account lockout support
-- Password history for preventing password reuse
CREATE TABLE IF NOT EXISTS password_histories (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    password_hash   VARCHAR(255) NOT NULL,
    created_at      TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_password_histories_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_password_histories_user_id ON password_histories(user_id);

COMMENT ON TABLE password_histories IS '用户密码历史记录表，用于防止密码重用';

-- Add account lockout fields to users table
ALTER TABLE users ADD COLUMN IF NOT EXISTS failed_login_attempts INTEGER DEFAULT 0;
ALTER TABLE users ADD COLUMN IF NOT EXISTS locked_until TIMESTAMP;

COMMENT ON COLUMN users.failed_login_attempts IS '连续登录失败次数';
COMMENT ON COLUMN users.locked_until IS '账户锁定截止时间，NULL表示未锁定';
