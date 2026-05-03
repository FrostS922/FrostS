package com.frosts.testplatform.dto;

import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
public class RequirementCoverageDTO {

    private Long requirementId;
    private String requirementNumber;
    private String requirementTitle;
    private Integer totalTestCases;
    private Integer activeTestCases;
    private Double coverageRate;
    private Map<String, Long> testCaseStatusDistribution;
    private List<TestCaseSummary> testCases;

    @Data
    public static class TestCaseSummary {
        private Long id;
        private String caseNumber;
        private String title;
        private String type;
        private String priority;
        private String status;
    }
}
