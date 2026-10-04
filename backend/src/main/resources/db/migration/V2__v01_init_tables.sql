-- MTC V0.1 业务表初始化
-- 项目、测试用例、测试脚本、测试数据、会话、消息、文档

-- 项目表
CREATE TABLE IF NOT EXISTS mtc_project (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description TEXT,
    status      VARCHAR(20) NOT NULL DEFAULT 'active',
    created_by  BIGINT,
    created_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at  TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_project IS '项目表';
COMMENT ON COLUMN mtc_project.status IS '项目状态：active/archived';

CREATE INDEX IF NOT EXISTS idx_project_created_by ON mtc_project(created_by);

-- 测试用例表
CREATE TABLE IF NOT EXISTS mtc_test_case (
    id              BIGSERIAL PRIMARY KEY,
    project_id      BIGINT NOT NULL,
    title           VARCHAR(200) NOT NULL,
    module          VARCHAR(100),
    priority        VARCHAR(20) NOT NULL DEFAULT 'medium',
    precondition    TEXT,
    steps           TEXT,
    expected_result TEXT,
    type            VARCHAR(20) NOT NULL DEFAULT 'functional',
    status          VARCHAR(20) NOT NULL DEFAULT 'draft',
    source          VARCHAR(20) NOT NULL DEFAULT 'ai_generated',
    conversation_id BIGINT,
    created_by      BIGINT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at      TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_test_case IS '测试用例表';
COMMENT ON COLUMN mtc_test_case.priority IS '优先级：high/medium/low';
COMMENT ON COLUMN mtc_test_case.type IS '用例类型：functional/performance/security等';
COMMENT ON COLUMN mtc_test_case.status IS '状态：draft/reviewed/approved/obsolete';
COMMENT ON COLUMN mtc_test_case.source IS '来源：manual/ai_generated/imported';

CREATE INDEX IF NOT EXISTS idx_test_case_project_id ON mtc_test_case(project_id);
CREATE INDEX IF NOT EXISTS idx_test_case_module ON mtc_test_case(module);

-- 测试脚本表
CREATE TABLE IF NOT EXISTS mtc_test_script (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    test_case_id BIGINT,
    name         VARCHAR(200) NOT NULL,
    framework    VARCHAR(20) NOT NULL DEFAULT 'playwright',
    language     VARCHAR(20) NOT NULL DEFAULT 'typescript',
    content      TEXT,
    status       VARCHAR(20) NOT NULL DEFAULT 'draft',
    created_by   BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_test_script IS '测试脚本表';
COMMENT ON COLUMN mtc_test_script.framework IS '测试框架：playwright/selenium/cypress等';
COMMENT ON COLUMN mtc_test_script.language IS '编程语言：typescript/python/java等';
COMMENT ON COLUMN mtc_test_script.status IS '状态：draft/generating/ready/failed';

CREATE INDEX IF NOT EXISTS idx_script_project_id ON mtc_test_script(project_id);
CREATE INDEX IF NOT EXISTS idx_script_case_id ON mtc_test_script(test_case_id);

-- 测试数据表
CREATE TABLE IF NOT EXISTS mtc_test_data (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    test_case_id BIGINT,
    name         VARCHAR(200) NOT NULL,
    data_type    VARCHAR(20) NOT NULL DEFAULT 'json',
    content      TEXT,
    created_by   BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_test_data IS '测试数据表';
COMMENT ON COLUMN mtc_test_data.data_type IS '数据类型：json/csv/xml/yaml等';

CREATE INDEX IF NOT EXISTS idx_data_project_id ON mtc_test_data(project_id);

-- 会话表
CREATE TABLE IF NOT EXISTS mtc_conversation (
    id         BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL,
    title      VARCHAR(200),
    type       VARCHAR(20) NOT NULL DEFAULT 'test_design',
    created_by BIGINT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_conversation IS '会话表';
COMMENT ON COLUMN mtc_conversation.type IS '会话类型：test_design/script_generation/data_generation等';

CREATE INDEX IF NOT EXISTS idx_conv_project_id ON mtc_conversation(project_id);

-- 消息表（会话历史）
CREATE TABLE IF NOT EXISTS mtc_message (
    id              BIGSERIAL PRIMARY KEY,
    conversation_id BIGINT NOT NULL,
    role            VARCHAR(20) NOT NULL,
    content         TEXT,
    token_count     INT,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE mtc_message IS '消息表（会话历史）';
COMMENT ON COLUMN mtc_message.role IS '角色：user/assistant/system';

CREATE INDEX IF NOT EXISTS idx_msg_conv_id ON mtc_message(conversation_id);

-- 文档表（RAG用）
CREATE TABLE IF NOT EXISTS mtc_document (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    filename     VARCHAR(255) NOT NULL,
    file_type    VARCHAR(50),
    file_size    BIGINT,
    storage_path VARCHAR(500),
    status       VARCHAR(20) NOT NULL DEFAULT 'uploaded',
    chunk_count  INT NOT NULL DEFAULT 0,
    created_by   BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP WITH TIME ZONE
);

COMMENT ON TABLE mtc_document IS '文档表（RAG用）';
COMMENT ON COLUMN mtc_document.status IS '状态：uploaded/processing/ready/failed';

CREATE INDEX IF NOT EXISTS idx_doc_project_id ON mtc_document(project_id);
