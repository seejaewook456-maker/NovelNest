package org.example.global.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
public class ApiResponse {

    private final String message;

    // null이면 JSON 응답에서 제외 (회원가입처럼 data가 없는 경우를 위해)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final Object data;

    private ApiResponse(String message, Object data) {
        this.message = message;
        this.data = data;
    }

    public static ApiResponse of(String message) {
        return new ApiResponse(message, null);
    }

    public static ApiResponse of(String message, Object data) {
        return new ApiResponse(message, data);
    }
}
