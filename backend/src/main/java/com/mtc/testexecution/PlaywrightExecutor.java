package com.mtc.testexecution;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.entity.TestRunResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Playwright 执行器
 * 通过子进程调用 Playwright CLI 执行测试脚本
 */
@Component
public class PlaywrightExecutor {

    private static final Logger log = LoggerFactory.getLogger(PlaywrightExecutor.class);

    @Value("${mtc.playwright.work-dir:${java.io.tmpdir}/mtc-playwright}")
    private String workDir;

    @Value("${mtc.playwright.timeout-seconds:300}")
    private int timeoutSeconds;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 Playwright 测试脚本
     *
     * @param scriptContent  脚本内容
     * @param scriptName     脚本名称
     * @param baseUrl        基础 URL
     * @param logConsumer    日志输出消费者（用于 SSE 推送）
     * @return 执行结果（用例级别）
     */
    public ExecutionResult execute(
            String scriptContent,
            String scriptName,
            String baseUrl,
            Consumer<String> logConsumer
    ) throws IOException, InterruptedException {
        // 1. 准备工作目录
        Path runDir = prepareWorkDir(scriptName);
        log.info("Playwright 执行目录: {}", runDir);

        try {
            // 2. 写入脚本文件（剥离 markdown 代码块标记）
            Path specFile = runDir.resolve("tests").resolve(scriptName);
            String cleanContent = stripMarkdownCodeBlocks(scriptContent);
            Files.writeString(specFile, cleanContent, StandardCharsets.UTF_8);
            log.info("写入脚本文件: {}", specFile);

            // 3. 写入 playwright.config.ts
            String configContent = generateConfig(baseUrl, runDir);
            Path configFile = runDir.resolve("playwright.config.ts");
            Files.writeString(configFile, configContent, StandardCharsets.UTF_8);

            // 4. 确保依赖已安装
            installDependencies(runDir, logConsumer);

            // 5. 修复代码换行（AI 生成的压缩代码单行过长会导致 Playwright 无法扫描）
            fixCodeLineBreaks(specFile);

            // 6. 格式化脚本（修复缩进、空格等）
            formatScript(runDir, specFile, logConsumer);

            // 7. 安装浏览器（如需要）
            installBrowsers(runDir, logConsumer);

            // 8. 执行测试
            return runTests(runDir, specFile, logConsumer);

        } finally {
            // 7. 清理工作目录（保留截图等产物在结果里）
            // 暂时不删，便于调试；生产环境可加配置控制
        }
    }

    private static final java.util.regex.Pattern LANG_PATTERN =
            java.util.regex.Pattern.compile("^(typescript|ts|javascript|js|python|java|bash|shell|json)");

    /**
     * 剥离脚本内容中的 markdown 代码块标记
     * 支持标准格式（```typescript\n代码\n```）和非标准格式（```typescript代码```）
     */
    private String stripMarkdownCodeBlocks(String content) {
        if (content == null || content.isBlank()) {
            return content;
        }
        String trimmed = content.trim();

        // 处理开头的 ```language 标记
        if (trimmed.startsWith("```")) {
            // 去掉开头的3个反引号
            trimmed = trimmed.substring(3);
            // 找到第一个换行符；如果在开头20个字符内有换行，去掉语言标识行
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline >= 0 && firstNewline <= 20) {
                // 标准格式：第一行是语言标识
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                // 非标准格式：语言标识和代码在同一行，用正则匹配已知语言
                java.util.regex.Matcher m = LANG_PATTERN.matcher(trimmed);
                if (m.find()) {
                    trimmed = trimmed.substring(m.end());
                }
            }

            // 去掉结尾的 ```
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
            // 再去掉结尾可能的换行和空格
            trimmed = trimmed.trim();
        }
        return trimmed;
    }

    private Path prepareWorkDir(String scriptName) throws IOException {
        String runId = "run-" + System.currentTimeMillis();
        Path runDir = Paths.get(workDir, runId);
        Files.createDirectories(runDir.resolve("tests"));
        Files.createDirectories(runDir.resolve("test-results"));
        return runDir;
    }

    private String generateConfig(String baseUrl, Path runDir) {
        return """
            import { defineConfig, devices } from '@playwright/test';

            export default defineConfig({
              testDir: './tests',
              fullyParallel: false,
              reporter: [
                ['json', { outputFile: 'test-results/results.json' }],
                ['list']
              ],
              use: {
                baseURL: %s,
                screenshot: 'only-on-failure',
                trace: 'retain-on-failure',
              },
              projects: [
                {
                  name: 'chromium',
                  use: { ...devices['Desktop Chrome'] },
                },
              ],
              timeout: 60000,
            });
            """.formatted(
                baseUrl != null && !baseUrl.isBlank()
                    ? "'" + baseUrl.replace("'", "\\'") + "'"
                    : "undefined"
            );
    }

    private void installDependencies(Path runDir, Consumer<String> logConsumer)
            throws IOException, InterruptedException {
        // 检查 node_modules 是否存在
        Path nodeModules = runDir.resolve("node_modules");
        if (Files.exists(nodeModules)) {
            return;
        }

        // 写入 package.json
        String pkgJson = """
            {
              "name": "mtc-playwright-runner",
              "version": "1.0.0",
              "private": true,
              "devDependencies": {
                "@playwright/test": "^1.63.0",
                "prettier": "^3.4.0"
              }
            }
            """;
        Files.writeString(runDir.resolve("package.json"), pkgJson, StandardCharsets.UTF_8);

        logConsumer.accept("[系统] 正在安装 Playwright 依赖...");
        ProcessBuilder pb = new ProcessBuilder("npm", "install", "--no-audit", "--no-fund");
        pb.directory(runDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logConsumer.accept("[npm] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("npm install 失败，退出码: " + exitCode);
        }
        logConsumer.accept("[系统] 依赖安装完成");
    }

    private void fixCodeLineBreaks(Path specFile) throws IOException {
        String content = Files.readString(specFile, StandardCharsets.UTF_8);
        // 如果已经有很多换行了，不需要处理
        long newlineCount = content.chars().filter(c -> c == '\n').count();
        if (newlineCount > 10) {
            return;
        }
        // 在分号后加换行（保留分号）
        content = content.replaceAll(";", ";\n");
        // 在左大括号后加换行
        content = content.replaceAll("\\{", "{\n");
        // 在右大括号前后加换行
        content = content.replaceAll("\\}", "\n}\n");
        // 在逗号后加空格（帮助 prettier 更好地格式化）
        content = content.replaceAll(",([^\\s])", ", $1");
        // 清理多余的空行
        content = content.replaceAll("\n{3,}", "\n\n");
        Files.writeString(specFile, content, StandardCharsets.UTF_8);
    }

    private void formatScript(Path runDir, Path specFile, Consumer<String> logConsumer)
            throws IOException, InterruptedException {
        logConsumer.accept("[系统] 格式化脚本...");

        // 脚本在 tests/ 子目录下，prettier 从 runDir 根目录运行
        String relativePath = runDir.relativize(specFile).toString();

        ProcessBuilder pb = new ProcessBuilder(
                "npx", "prettier", "--write", relativePath
        );
        pb.directory(runDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // prettier 输出比较简洁，直接透传
                logConsumer.accept("[prettier] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Prettier 格式化退出码: {}，将尝试继续执行", exitCode);
            logConsumer.accept("[系统] 脚本格式化失败，将尝试直接执行");
        } else {
            logConsumer.accept("[系统] 脚本格式化完成");
        }
    }

    private void installBrowsers(Path runDir, Consumer<String> logConsumer)
            throws IOException, InterruptedException {
        logConsumer.accept("[系统] 检查浏览器环境...");

        ProcessBuilder pb = new ProcessBuilder(
                "npx", "playwright", "install", "chromium", "--with-deps"
        );
        pb.directory(runDir.toFile());
        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                logConsumer.accept("[playwright] " + line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.warn("Playwright 浏览器安装退出码: {}，将尝试继续执行", exitCode);
            logConsumer.accept("[系统] 浏览器安装可能未完全成功，将尝试执行测试");
        } else {
            logConsumer.accept("[系统] 浏览器环境就绪");
        }
    }

    private ExecutionResult runTests(
            Path runDir,
            Path specFile,
            Consumer<String> logConsumer
    ) throws IOException, InterruptedException {
        logConsumer.accept("[系统] 开始执行测试...");

        ProcessBuilder pb = new ProcessBuilder(
                "npx", "playwright", "test",
                specFile.getFileName().toString()
        );
        pb.directory(runDir.toFile());
        pb.redirectErrorStream(true);

        long startTime = System.currentTimeMillis();
        Process process = pb.start();

        StringBuilder outputBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                outputBuilder.append(line).append("\n");
                logConsumer.accept(line);
            }
        }

        boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
        long durationMs = System.currentTimeMillis() - startTime;

        if (!finished) {
            process.destroyForcibly();
            throw new RuntimeException("测试执行超时（" + timeoutSeconds + "秒）");
        }

        int exitCode = process.exitValue();
        log.info("Playwright 执行完成，退出码: {}, 耗时: {}ms", exitCode, durationMs);

        // 解析 JSON 结果
        Path resultFile = runDir.resolve("test-results").resolve("results.json");
        ExecutionResult result = new ExecutionResult();
        result.setExitCode(exitCode);
        result.setDurationMs(durationMs);
        result.setWorkDir(runDir.toString());

        if (Files.exists(resultFile)) {
            try {
                JsonNode root = objectMapper.readTree(resultFile.toFile());
                JsonNode suites = root.path("suites");
                List<TestRunResult> testResults = new ArrayList<>();
                parseSuites(suites, testResults, runDir);
                result.setResults(testResults);

                // 统计
                int total = testResults.size();
                int passed = 0, failed = 0, skipped = 0;
                for (TestRunResult r : testResults) {
                    switch (r.getStatus()) {
                        case "passed" -> passed++;
                        case "failed" -> failed++;
                        case "skipped" -> skipped++;
                        default -> { }
                    }
                }
                result.setTotalTests(total);
                result.setPassedTests(passed);
                result.setFailedTests(failed);
                result.setSkippedTests(skipped);
            } catch (Exception e) {
                log.error("解析 Playwright 结果失败", e);
                logConsumer.accept("[错误] 解析测试结果失败: " + e.getMessage());
            }
        } else {
            log.warn("未找到测试结果文件: {}", resultFile);
            logConsumer.accept("[警告] 未找到结果文件，可能执行未完成");
        }

        return result;
    }

    private void parseSuites(JsonNode suites, List<TestRunResult> results, Path runDir) {
        if (suites == null || !suites.isArray()) {
            return;
        }
        for (JsonNode suite : suites) {
            JsonNode specs = suite.path("specs");
            if (specs.isArray()) {
                for (JsonNode spec : specs) {
                    String title = spec.path("title").asText();
                    String file = spec.path("file").asText();
                    JsonNode tests = spec.path("tests");
                    if (tests.isArray() && tests.size() > 0) {
                        JsonNode test = tests.get(0);
                        JsonNode resultsNode = test.path("results");
                        if (resultsNode.isArray() && resultsNode.size() > 0) {
                            JsonNode firstResult = resultsNode.get(0);
                            TestRunResult result = new TestRunResult();
                            result.setTitle(title);
                            result.setFile(file);
                            result.setStatus(firstResult.path("status").asText("unknown"));
                            result.setDurationMs(firstResult.path("duration").asLong(0));

                            // 错误信息
                            JsonNode errors = firstResult.path("errors");
                            if (errors.isArray() && errors.size() > 0) {
                                result.setErrorMessage(errors.get(0).path("message").asText(""));
                            }

                            // 截图
                            JsonNode attachments = firstResult.path("attachments");
                            if (attachments.isArray()) {
                                for (JsonNode att : attachments) {
                                    if ("screenshot".equals(att.path("name").asText())) {
                                        String path = att.path("path").asText("");
                                        if (!path.isBlank()) {
                                            // 存相对路径或绝对路径
                                            result.setScreenshotPath(path);
                                        }
                                    }
                                }
                            }

                            OffsetDateTime now = OffsetDateTime.now();
                            result.setCreatedAt(now);
                            result.setStartedAt(now);
                            result.setFinishedAt(now);

                            results.add(result);
                        }
                    }
                }
            }
            // 递归解析嵌套 suites
            parseSuites(suite.path("suites"), results, runDir);
        }
    }

    /**
     * 执行结果封装
     */
    public static class ExecutionResult {
        private int exitCode;
        private long durationMs;
        private int totalTests;
        private int passedTests;
        private int failedTests;
        private int skippedTests;
        private String workDir;
        private List<TestRunResult> results = new ArrayList<>();

        public int getExitCode() { return exitCode; }
        public void setExitCode(int exitCode) { this.exitCode = exitCode; }

        public long getDurationMs() { return durationMs; }
        public void setDurationMs(long durationMs) { this.durationMs = durationMs; }

        public int getTotalTests() { return totalTests; }
        public void setTotalTests(int totalTests) { this.totalTests = totalTests; }

        public int getPassedTests() { return passedTests; }
        public void setPassedTests(int passedTests) { this.passedTests = passedTests; }

        public int getFailedTests() { return failedTests; }
        public void setFailedTests(int failedTests) { this.failedTests = failedTests; }

        public int getSkippedTests() { return skippedTests; }
        public void setSkippedTests(int skippedTests) { this.skippedTests = skippedTests; }

        public String getWorkDir() { return workDir; }
        public void setWorkDir(String workDir) { this.workDir = workDir; }

        public List<TestRunResult> getResults() { return results; }
        public void setResults(List<TestRunResult> results) { this.results = results; }
    }
}
