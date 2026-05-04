package com.frosts.testplatform.dto.security;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "登录趋势数据点")
public class LoginTrendPoint {

    @Schema(description = "日期", example = "2026-05-04")
    private String date;

    @Schema(description = "登录成功次数", example = "12")
    private long successes;

    @Schema(description = "登录失败次数", example = "2")
    private long failures;
}
