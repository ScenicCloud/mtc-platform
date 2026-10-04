package com.mtc.testdesign.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class GenerateTestCaseRequest {

    @NotNull(message = "projectId 不能为空")
    private Long projectId;

    @NotBlank(message = "requirement 不能为空")
    @Size(min = 1, max = 5000, message = "requirement 长度必须在 1-5000 之间")
    private String requirement;

    private List<Long> docIds;

    private String context;

    public Long getProjectId() {
        return projectId;
    }

    public void setProjectId(Long projectId) {
        this.projectId = projectId;
    }

    public String getRequirement() {
        return requirement;
    }

    public void setRequirement(String requirement) {
        this.requirement = requirement;
    }

    public List<Long> getDocIds() {
        return docIds;
    }

    public void setDocIds(List<Long> docIds) {
        this.docIds = docIds;
    }

    public String getContext() {
        return context;
    }

    public void setContext(String context) {
        this.context = context;
    }
}
