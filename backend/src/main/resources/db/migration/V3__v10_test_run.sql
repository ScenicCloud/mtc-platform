-- MTC V1.0 测试执行相关表
-- 测试运行记录、用例执行结果

-- 测试运行表
CREATE TABLE IF NOT EXISTS mtc_test_run (
    id           BIGSERIAL PRIMARY KEY,
    project_id   BIGINT NOT NULL,
    script_id    BIGINT NOT NULL,
    script_name  VARCHAR(200),
    status       VARCHAR(20) NOT NULL DEFAULT 'pending',
    -- pending / running / completed / failed / cancelled
    total_tests  INT NOT NULL DEFAULT 0,
    passed_tests INT NOT NULL DEFAULT 0,
    failed_tests INT NOT NULL DEFAULT 0,
    skipped_tests INT NOT NULL DEFAULT 0,
    duration_ms  BIGINT,
    error_message TEXT,
    base_url     VARCHAR(500),
    created_by   BIGINT,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at   TIMESTAMP WITH TIME ZONE,
    finished_at  TIMESTAMP WITH TIME ZONE,
    updated_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE mtc_test_run IS '测试运行记录表';
COMMENT ON COLUMN mtc_test_run.status IS '运行状态：pending/running/completed/failed/cancelled';
COMMENT ON COLUMN mtc_test_run.total_tests IS '总用例数';
COMMENT ON COLUMN mtc_test_run.passed_tests IS '通过数';
COMMENT ON COLUMN mtc_test_run.failed_tests IS '失败数';
COMMENT ON COLUMN mtc_test_run.skipped_tests IS '跳过数';
COMMENT ON COLUMN mtc_test_run.duration_ms IS '执行耗时（毫秒）';
COMMENT ON COLUMN mtc_test_run.base_url IS '测试目标基础 URL';

CREATE INDEX IF NOT EXISTS idx_run_project_id ON mtc_test_run(project_id);
CREATE INDEX IF NOT EXISTS idx_run_script_id ON mtc_test_run(script_id);
CREATE INDEX IF NOT EXISTS idx_run_status ON mtc_test_run(status);

-- 测试用例执行结果表
CREATE TABLE IF NOT EXISTS mtc_test_run_result (
    id           BIGSERIAL PRIMARY KEY,
    run_id       BIGINT NOT NULL,
    title        VARCHAR(500) NOT NULL,
    file         VARCHAR(500),
    status       VARCHAR(20) NOT NULL,
    -- passed / failed / skipped / timedOut
    duration_ms  BIGINT,
    error_message TEXT,
    screenshot_path VARCHAR(500),
    started_at   TIMESTAMP WITH TIME ZONE,
    finished_at  TIMESTAMP WITH TIME ZONE,
    created_at   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE mtc_test_run_result IS '测试用例执行结果表';
COMMENT ON COLUMN mtc_test_run_result.status IS '结果状态：passed/failed/skipped/timedOut';

CREATE INDEX IF NOT EXISTS idx_result_run_id ON mtc_test_run_result(run_id);
CREATE INDEX IF NOT EXISTS idx_result_status ON mtc_test_run_result(status);
