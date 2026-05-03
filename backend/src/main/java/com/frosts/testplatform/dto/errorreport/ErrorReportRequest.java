package com.frosts.testplatform.dto.errorreport;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.Map;

@Data
public class ErrorReportRequest {

    @NotBlank(message = "错误信息不能为空")
    @Size(max = 2000, message = "错误信息长度不能超过2000")
    private String message;

    @Size(max = 10000, message = "堆栈信息长度不能超过10000")
    private String stack;

    @Size(max = 500, message = "页面URL长度不能超过500")
    private String url;

    private Long timestamp;

    @Size(max = 1000, message = "UserAgent长度不能超过1000")
    private String userAgent;

    private Map<String, Object> extra;
}
