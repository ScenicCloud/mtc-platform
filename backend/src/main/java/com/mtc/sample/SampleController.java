package com.mtc.sample;

import com.mtc.ai.AiClient;
import com.mtc.common.BusinessException;
import com.mtc.common.ErrorCode;
import com.mtc.common.Result;
import com.mtc.sample.dto.AnalyzeRequest;
import com.mtc.sample.dto.SpaceVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;

@RestController
@RequestMapping("/api/v1/sample")
@Tag(name = "示例（占位）", description = "占位接口，验证链路用，真业务上线后删除")
public class SampleController {

    private final SampleService sampleService;
    private final AiClient aiClient;

    public SampleController(SampleService sampleService, AiClient aiClient) {
        this.sampleService = sampleService;
        this.aiClient = aiClient;
    }

    @GetMapping("/spaces")
    @Operation(summary = "获取空间列表（占位）")
    public Result<List<SpaceVO>> getSpaces() {
        return Result.ok(sampleService.getSpaces());
    }

    @PostMapping(value = "/analyze", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "流式分析（占位）")
    public SseEmitter analyze(@Valid @RequestBody AnalyzeRequest request,
                              Authentication authentication) {
        // 校验 spaceId 存在性
        if (!sampleService.spaceExists(request.getSpaceId())) {
            throw new BusinessException(ErrorCode.PARAM_OUT_OF_RANGE, "spaceId 不存在");
        }

        Long userId = (Long) authentication.getPrincipal();
        return aiClient.streamChat(request.getPrompt(), userId);
    }
}
