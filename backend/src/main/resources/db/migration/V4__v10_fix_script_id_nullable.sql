-- V3 修复：script_id 允许为空（直接执行脚本内容的场景下没有 script_id）
ALTER TABLE mtc_test_run ALTER COLUMN script_id DROP NOT NULL;
