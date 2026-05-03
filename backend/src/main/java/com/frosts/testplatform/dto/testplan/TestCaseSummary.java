package com.frosts.testplatform.dto.testplan;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TestCaseSummary {

    private Long id;
    private String caseNumber;
    private String title;
    private String type;
    private String priority;
    private String moduleName;
}
