package com.mtc.testexecution;

import com.mtc.entity.TestRun;
import com.mtc.entity.TestScript;
import com.mtc.testscript.TestScriptService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * 测试执行编排服务
 * 负责将脚本写入、执行、结果解析、数据库更新串联起来
 */
@Service
public class TestExecutionService {

    private static final Logger log = LoggerFactory.getLogger(TestExecutionService.class);

    private final TestScriptService testScriptService;
    private final TestRunService testRunService;
    private final PlaywrightExecutor playwrightExecutor;
    private final TaskExecutor taskExecutor;

    // 运行中的 SSE emitter（用于取消执行）
    private final Map<Long, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    public TestExecutionService(
            TestScriptService testScriptService,
            TestRunService testRunService,
            PlaywrightExecutor playwrightExecutor,
            TaskExecutor taskExecutor
    ) {
        this.testScriptService = testScriptService;
        this.testRunService = testRunService;
        this.playwrightExecutor = playwrightExecutor;
        this.taskExecutor = taskExecutor;
    }

    /**
     * 触发执行（同步创建记录，异步执行）
     *
     * @return SSE emitter 用于推送执行进度
     */
    public SseEmitter executeScript(Long scriptId, String baseUrl) {
        // 1. 获取脚本
        TestScript script = testScriptService.getTestScriptById(scriptId);

        // 2. 创建运行记录
        TestRun run = testRunService.createRun(
                script.getProjectId(),
                script.getId(),
                script.getName(),
                baseUrl != null && !baseUrl.isBlank() ? baseUrl : script.getContent()
                        .lines()
                        .filter(l -> l.contains("baseURL") || l.contains("baseUrl"))
                        .findFirst()
                        .orElse("")
        );

        // 3. 创建 SSE emitter（超时 10 分钟）
        SseEmitter emitter = new SseEmitter(600_000L);
        activeEmitters.put(run.getId(), emitter);

        emitter.onCompletion(() -> activeEmitters.remove(run.getId()));
        emitter.onTimeout(() -> activeEmitters.remove(run.getId()));
        emitter.onError((e) -> activeEmitters.remove(run.getId()));

        // 4. 异步执行
        taskExecutor.execute(() -> doExecute(run.getId(), script.getContent(), script.getName(), baseUrl));

        // 5. 先推送一个 start 事件
        try {
            emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"runId\":" + run.getId() + ",\"scriptName\":\"" + escapeJson(script.getName()) + "\"}"));
        } catch (IOException e) {
            log.warn("发送 start 事件失败", e);
        }

        return emitter;
    }

    /**
     * 使用临时脚本执行（不保存脚本，直接执行）
     */
    public SseEmitter executeScriptContent(
            Long projectId,
            String scriptContent,
            String scriptName,
            String baseUrl
    ) {
        // 1. 创建运行记录
        TestRun run = testRunService.createRun(projectId, null, scriptName, baseUrl);

        // 2. 创建 SSE emitter
        SseEmitter emitter = new SseEmitter(600_000L);
        activeEmitters.put(run.getId(), emitter);

        emitter.onCompletion(() -> activeEmitters.remove(run.getId()));
        emitter.onTimeout(() -> activeEmitters.remove(run.getId()));
        emitter.onError((e) -> activeEmitters.remove(run.getId()));

        // 3. 异步执行
        taskExecutor.execute(() -> doExecute(run.getId(), scriptContent, scriptName, baseUrl));

        // 4. 推送 start 事件
        try {
            emitter.send(SseEmitter.event()
                    .name("start")
                    .data("{\"runId\":" + run.getId() + ",\"scriptName\":\"" + escapeJson(scriptName) + "\"}"));
        } catch (IOException e) {
            log.warn("发送 start 事件失败", e);
        }

        return emitter;
    }

    /**
     * 取消执行
     */
    public void cancelRun(Long runId) {
        SseEmitter emitter = activeEmitters.get(runId);
        if (emitter != null) {
            emitter.complete();
            testRunService.markFailed(runId, "用户取消执行");
        }
    }

    private void doExecute(Long runId, String scriptContent, String scriptName, String baseUrl) {
        SseEmitter emitter = activeEmitters.get(runId);
        if (emitter == null) {
            return;
        }

        try {
            // 标记运行中
            testRunService.markRunning(runId);
            sendEvent(emitter, "status", "{\"status\":\"running\"}");

            // 日志消费者
            Consumer<String> logConsumer = (line) -> {
                sendEvent(emitter, "log", "{\"message\":\"" + escapeJson(line) + "\"}");
            };

            // 执行
            PlaywrightExecutor.ExecutionResult result = playwrightExecutor.execute(
                    scriptContent,
                    scriptName,
                    baseUrl,
                    logConsumer
            );

            // 保存结果
            testRunService.markCompleted(runId, result);

            // 推送完成事件
            String summaryJson = String.format(
                    "{\"status\":\"%s\",\"total\":%d,\"passed\":%d,\"failed\":%d,\"skipped\":%d,\"durationMs\":%d}",
                    result.getFailedTests() > 0 ? "failed" : "completed",
                    result.getTotalTests(),
                    result.getPassedTests(),
                    result.getFailedTests(),
                    result.getSkippedTests(),
                    result.getDurationMs()
            );
            sendEvent(emitter, "done", summaryJson);

            emitter.complete();

        } catch (Exception e) {
            log.error("测试执行异常: runId={}", runId, e);
            testRunService.markFailed(runId, e.getMessage());
            sendEvent(emitter, "error", "{\"message\":\"" + escapeJson(e.getMessage()) + "\"}");
            emitter.completeWithError(e);
        }
    }

    private void sendEvent(SseEmitter emitter, String name, String data) {
        try {
            emitter.send(SseEmitter.event().name(name).data(data));
        } catch (IOException e) {
            log.debug("SSE 发送失败（可能客户端已断开）: {}", e.getMessage());
        }
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}
