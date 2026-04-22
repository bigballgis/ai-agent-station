-- V15: 登录审计日志表
CREATE TABLE login_logs (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(100),
    user_id BIGINT,
    login_type VARCHAR(20) NOT NULL,
    ip VARCHAR(50),
    location VARCHAR(200),
    browser VARCHAR(50),
    os VARCHAR(50),
    status VARCHAR(20),
    message VARCHAR(500),
    login_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    tenant_id BIGINT
);

CREATE INDEX idx_login_logs_user ON login_logs(user_id);
CREATE INDEX idx_login_logs_time ON login_logs(login_time);
CREATE INDEX idx_login_logs_tenant ON login_logs(tenant_id);
