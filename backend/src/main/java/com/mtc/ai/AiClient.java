package com.mtc.ai;

import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.common.TraceIdHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    }

    public SseEmitter streamChat(String message, Long userId) {
        String traceId = TraceIdHolder.get();
        String sessionId = UUID.randomUUID().toString();

        SseEmitter emitter = new SseEmitter(0L); // 无超时，由空闲看门狗控制

        // 构造请求体
        String body = String.format(
                "{\"session_id\":\"%s\",\"message\":\"%s\",\"trace_id\":\"%s\",\"user_id\":%d}",
                sessionId, escapeJson(message), traceId, userId
        );

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/chat/stream"))
                .header("Content-Type", "application/json")
                .header("X-Trace-Id", traceId)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        executor.submit(() -> {
            try {
                HttpResponse<java.util.stream.Stream<String>> response = httpClient.send(
                        request,
                        HttpResponse.BodyHandlers.ofLines()
                );

                int statusCode = response.statusCode();
                if (statusCode != 200) {
                    // A 段：还没开始吐数据就失败了
                    log.warn("AI 服务返回非 200: {}", statusCode);
                    emitter.completeWithError(
                            new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE));
                    return;
                }

                // B 段：流式转发
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
                                    // 首帧不是 meta，不合法
                                    log.warn("AI 首帧事件不是 meta: {}", result.event);
                                    emitter.completeWithError(
                                            new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE));
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

                        // 空闲超时看门狗
                        if (System.currentTimeMillis() - lastEventTime > readTimeoutMs) {
                            log.warn("AI 流式响应空闲超时");
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("{\"code\":5001,\"message\":\"服务响应超时\"}"));
                            emitter.complete();
                            return;
                        }
                    }

                    // 流结束但没收到 done/error
                    if (metaReceived) {
                        log.warn("AI 流异常结束，未收到 done 或 error");
                        emitter.send(SseEmitter.event()
                                .name("error")
                                .data("{\"code\":5001,\"message\":\"服务连接异常中断\"}"));
                    }
                    emitter.complete();

                } catch (Exception e) {
                    log.warn("流式转发异常: {}", e.getMessage());
                    try {
                        if (metaReceived) {
                            emitter.send(SseEmitter.event()
                                    .name("error")
                                    .data("{\"code\":5001,\"message\":\"服务连接异常\"}"));
                        }
                    } catch (Exception ignored) {
                    }
                    emitter.complete();
                }

            } catch (Exception e) {
                log.warn("调用 AI 服务失败: {}", e.getMessage());
                emitter.completeWithError(
                        new BusinessException(ErrorCode.DEPENDENCY_UNAVAILABLE));
            }
        });

        emitter.onTimeout(() -> log.debug("SSE emitter timeout"));
        emitter.onError(e -> log.debug("SSE emitter error: {}", e.getMessage()));

        return emitter;
    }

    private String escapeJson(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> sb.append(c);
            }
        }
        return sb.toString();
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
