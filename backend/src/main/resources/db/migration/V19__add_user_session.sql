-- V19: Add user_sessions table for session management
CREATE TABLE IF NOT EXISTS user_sessions (
    id              BIGSERIAL PRIMARY KEY,
    user_id         BIGINT       NOT NULL,
    username        VARCHAR(100) NOT NULL,
    session_id      VARCHAR(255) NOT NULL UNIQUE,
    ip_address      VARCHAR(50),
    user_agent      VARCHAR(500),
    browser         VARCHAR(100),
    os              VARCHAR(100),
    login_time      TIMESTAMP    NOT NULL DEFAULT NOW(),
    last_access_time TIMESTAMP,
    expire_time     TIMESTAMP    NOT NULL,
    status          VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT fk_user_sessions_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Indexes for efficient querying
CREATE INDEX idx_user_sessions_user_id ON user_sessions(user_id);
CREATE UNIQUE INDEX idx_user_sessions_session_id ON user_sessions(session_id);
CREATE INDEX idx_user_sessions_status ON user_sessions(status);
CREATE INDEX idx_user_sessions_expire_time ON user_sessions(expire_time);

-- Comment
COMMENT ON TABLE user_sessions IS 'User session tracking for multi-device management';
COMMENT ON COLUMN user_sessions.session_id IS 'JWT jti claim or token identifier';
COMMENT ON COLUMN user_sessions.status IS 'Session status: ACTIVE, EXPIRED, LOGOUT, KICKED';
