package com.mtc.testdesign.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.Map;

public class GenerateTestScriptRequest {

    @NotNull(message = "projectId 不能为空")
    private Long projectId;

    @NotNull(message = "testCases 不能为空")
    private List<Map<String, Object>> testCases;

    private String module;

    private String baseUrl;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public List<Map<String, Object>> getTestCases() {
        return testCases;
    }

    public void setTestCases(List<Map<String, Object>> testCases) {
        this.testCases = testCases;
    }

    public String getModule() {
        return module;
    }

    public void setModule(String module) {
        this.module = module;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }
}
