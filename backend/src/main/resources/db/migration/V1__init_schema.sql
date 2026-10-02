-- MTC 初始化 Schema V1
-- 仅包含骨架阶段必需的表：sys_user

CREATE TABLE IF NOT EXISTS sys_user (
    id          BIGSERIAL PRIMARY KEY,
    username    VARCHAR(64) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    nickname    VARCHAR(128),
    roles       VARCHAR(255) DEFAULT '[]',
    enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE sys_user IS '用户表';
COMMENT ON COLUMN sys_user.roles IS '角色列表，JSON 数组字符串，如 ["ADMIN"]';

CREATE INDEX IF NOT EXISTS idx_sys_user_username ON sys_user(username);
