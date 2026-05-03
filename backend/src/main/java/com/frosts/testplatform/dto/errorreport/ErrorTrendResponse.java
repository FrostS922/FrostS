package com.frosts.testplatform.dto.errorreport;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ErrorTrendResponse {

    private List<String> dates;
    private List<Long> counts;
    private long total;
}
