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
@Schema(description = "安全概览响应")
public class SecurityOverviewResponse {

    @Schema(description = "今日登录成功次数", example = "15")
    private long todayLoginSuccesses;

    @Schema(description = "今日登录失败次数", example = "3")
    private long todayLoginFailures;

    @Schema(description = "今日异常IP数", example = "2")
    private long todayAnomalousIps;

    @Schema(description = "锁定账户数", example = "0")
    private long lockedAccounts;

    @Schema(description = "封禁IP数", example = "1")
    private long bannedIps;
}
