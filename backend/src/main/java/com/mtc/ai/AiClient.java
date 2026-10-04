package com.mtc.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.common.TraceIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.io.IOException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final String baseUrl;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final ObjectMapper objectMapper;

    public AiClient(
            @Value("${mtc.ai.base-url}") String baseUrl,
            @Value("${mtc.ai.connect-timeout-ms}") int connectTimeoutMs,
            @Value("${mtc.ai.read-timeout-ms}") int readTimeoutMs) {
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        this.connectTimeoutMs = connectTimeoutMs;
        this.readTimeoutMs = readTimeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .version(HttpClient.Version.HTTP_1_1)
                .build();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.objectMapper = new ObjectMapper();
    }

    public SseEmitter streamChat(String message, Long userId) {
        String traceId = TraceIdHolder.get();
        String sessionId = UUID.randomUUID().toString();

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("session_id", sessionId);
        bodyMap.put("message", message);
        bodyMap.put("trace_id", traceId);
        bodyMap.put("user_id", userId);

        String jsonBody = toJson(bodyMap);
        return streamPost("/chat/stream", jsonBody, traceId);
    }

    public SseEmitter streamTestCases(Long projectId, String requirement, List<Long> docIds, String context) {
        String traceId = TraceIdHolder.get();

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("project_id", projectId);
        bodyMap.put("requirement", requirement);
        bodyMap.put("doc_ids", docIds != null ? docIds : List.of());
        bodyMap.put("context", context != null ? context : "");
        bodyMap.put("trace_id", traceId);

        String jsonBody = toJson(bodyMap);
        return streamPost("/api/v1/test-design/test-cases/generate", jsonBody, traceId);
    }

    public SseEmitter streamTestScripts(Long projectId, List<Map<String, Object>> testCases, String module, String baseUrl) {
        String traceId = TraceIdHolder.get();

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("project_id", projectId);
        bodyMap.put("test_cases", testCases);
        bodyMap.put("module", module);
        bodyMap.put("base_url", baseUrl);
        bodyMap.put("trace_id", traceId);

        String jsonBody = toJson(bodyMap);
        return streamPost("/api/v1/test-design/test-scripts/generate", jsonBody, traceId);
    }

    public SseEmitter streamTestData(Long projectId, List<Map<String, Object>> testCases, String module) {
        String traceId = TraceIdHolder.get();

        Map<String, Object> bodyMap = new HashMap<>();
        bodyMap.put("project_id", projectId);
        bodyMap.put("test_cases", testCases);
        bodyMap.put("module", module);
        bodyMap.put("trace_id", traceId);

        String jsonBody = toJson(bodyMap);
        return streamPost("/api/v1/test-design/test-data/generate", jsonBody, traceId);
    }

    /**
     * 通用 SSE 流式 POST 请求
     *
     * @param path     请求路径（相对 baseUrl）
     * @param jsonBody 请求体 JSON 字符串
     * @param traceId  链路追踪 ID
     * @return SseEmitter
     */
    private SseEmitter streamPost(String path, String jsonBody, String traceId) {
        SseEmitter emitter = new SseEmitter(0L); // 无超时，由空闲看门狗控制

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + path))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, StandardCharsets.UTF_8))
                .build();

        executor.submit(() -> {
            try {
                HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofLines()
                );

                int statusCode = response.statusCode();
                if (statusCode != 200) {
                    log.warn("AI 服务返回非 200, path={}, status={}", path, statusCode);
                    sendErrorAndComplete(emitter, "AI 服务不可用（" + statusCode + "）");
                    return;
                }

                SseEventParser parser = new SseEventParser();
                boolean metaReceived = false;

                try (var stream = response.body()) {
                    var it = stream.iterator();
                    long lastEventTime = System.currentTimeMillis();

                    while (it.hasNext()) {
                        String line = it.next();
                        SseEventParser.ParseResult result = parser.feed(line);

                        if (result != null) {
                            lastEventTime = System.currentTimeMillis();

                            if (!metaReceived) {
                                if (!"meta".equals(result.event)) {
                                    log.warn("AI 首帧事件不是 meta: {}", result.event);
                                    sendErrorAndComplete(emitter, "AI 服务响应格式异常");
                                    return;
                                }
                                metaReceived = true;
                            }

                            emitter.send(SseEmitter.event()
                                    .name(result.event)
                                    .data(result.data));

                            if ("done".equals(result.event) || "error".equals(result.event)) {
                                emitter.complete();
                                return;
                            }
                        }

                        if (System.currentTimeMillis() - lastEventTime > readTimeoutMs) {
                            log.warn("AI 流式响应空闲超时, path={}", path);
                            sendErrorAndComplete(emitter, "服务响应超时");
                            return;
                        }
                    }

                    if (metaReceived) {
                        log.warn("AI 流异常结束，未收到 done 或 error, path={}", path);
                        sendErrorAndComplete(emitter, "服务连接异常中断");
                    } else {
                        sendErrorAndComplete(emitter, "服务无响应");
                    }
                    emitter.complete();

                } catch (Exception e) {
                    log.warn("流式转发异常, path={}: {}", path, e.getMessage());
                    sendErrorAndComplete(emitter, "服务连接异常");
                    emitter.complete();
                }

            } catch (Exception e) {
                log.warn("调用 AI 服务失败, path={}: {}", path, e.getMessage());
                sendErrorAndComplete(emitter, "AI 服务不可用");
            }
        });

        emitter.onTimeout(() -> log.debug("SSE emitter timeout"));
        emitter.onError(e -> log.debug("SSE emitter error: {}", e.getMessage()));

        return emitter;
    }

    /**
     * 发送 SSE error 事件并完成（避免 completeWithError 触发 Spring Security 异步 403）
     */
    private void sendErrorAndComplete(SseEmitter emitter, String message) {
        try {
            emitter.send(SseEmitter.event()
                    .name("error")
                    .data("{\"code\":5001,\"message\":\"" + message + "\"}"));
            emitter.complete();
        } catch (IOException e) {
            log.debug("发送 error 事件失败: {}", e.getMessage());
        }
    }

    private String toJson(Map<String, Object> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (JsonProcessingException e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "JSON 序列化失败");
        }
    }

    /**
     * 简单的 SSE 事件解析器
     * 处理 event: xxx 和 data: xxx 行，遇到空行返回一个完整事件
     */
    private static class SseEventParser {
        private String event;
        private final StringBuilder data = new StringBuilder();
        private boolean hasData;

        public record ParseResult(String event, String data) {}

        public ParseResult feed(String line) {
            if (line == null || line.isEmpty()) {
                // 空行 = 事件结束
                if (hasData) {
                    String e = event != null ? event : "message";
                    String d = data.toString();
                    reset();
                    return new ParseResult(e, d);
                }
                return null;
            }

            if (line.startsWith("event:")) {
                event = line.substring(6).trim();
            } else if (line.startsWith("data:")) {
                if (hasData) {
                    data.append('\n');
                }
                data.append(line.substring(5).trim());
                hasData = true;
            }
            return null;
        }

        private void reset() {
            event = null;
            data.setLength(0);
            hasData = false;
        }
    }
}
