package com.mtc.sample.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class AnalyzeRequest {

    @NotNull(message = "spaceId 不能为空")
    private Long spaceId;

    @NotBlank(message = "prompt 不能为空")
    @Size(min = 1, max = 2000, message = "prompt 长度必须在1-2000之间")
    private String prompt;

    public Long getSpaceId() {
        return spaceId;
    }

    public void setSpaceId(Long spaceId) {
        this.spaceId = spaceId;
    }

    public String getPrompt() {
        return prompt;
    }

    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }
}
