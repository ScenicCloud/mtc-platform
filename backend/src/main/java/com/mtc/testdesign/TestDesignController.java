package com.mtc.testdesign;

import com.mtc.ai.AiClient;
import com.mtc.testdesign.dto.GenerateTestCaseRequest;
import com.mtc.testdesign.dto.GenerateTestDataRequest;
import com.mtc.testdesign.dto.GenerateTestScriptRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/test-design")
@Tag(name = "测试设计", description = "AI 驱动的测试用例、脚本、数据生成接口")
public class TestDesignController {

    private final AiClient aiClient;

    public TestDesignController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @PostMapping(value = "/test-cases/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "生成测试用例（SSE 流式）")
    public SseEmitter generateTestCases(@Valid @RequestBody GenerateTestCaseRequest request) {
        return aiClient.streamTestCases(
                request.getProjectId(),
                request.getRequirement(),
                request.getDocIds(),
                request.getContext()
        );
    }

    @PostMapping(value = "/test-scripts/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "生成测试脚本（SSE 流式）")
    public SseEmitter generateTestScripts(@Valid @RequestBody GenerateTestScriptRequest request) {
        return aiClient.streamTestScripts(
                request.getProjectId(),
                request.getTestCases(),
                request.getModule(),
                request.getBaseUrl()
        );
    }

    @PostMapping(value = "/test-data/generate", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "生成测试数据（SSE 流式）")
    public SseEmitter generateTestData(@Valid @RequestBody GenerateTestDataRequest request) {
        return aiClient.streamTestData(
                request.getProjectId(),
                request.getTestCases(),
                request.getModule()
        );
    }
}
