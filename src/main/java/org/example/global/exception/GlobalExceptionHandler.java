package org.example.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // @Valid 검증 실패 (이메일 형식 오류, 빈 값 등)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .orElse("입력값이 올바르지 않습니다.");
        return ResponseEntity.badRequest().body(ApiResponse.of(message));
    }

    // 이메일 중복, 비밀번호 불일치 등 비즈니스 예외
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.of(e.getMessage()));
    }

    // 본인 소유가 아닌 리소스 접근 시도
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse> handleSecurityException(SecurityException e) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(ApiResponse.of(e.getMessage()));
    }

    // 잘못된 Enum 값 전송 시 (예: category에 "UNKNOWN" 입력)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest().body(ApiResponse.of("입력값 형식이 올바르지 않습니다."));
    }

    // 위에서 처리되지 않은 모든 예외 — 서버 오류를 ApiResponse 형식으로 반환해
    // 프론트엔드 fetchWithAuth가 json.message를 읽을 수 있도록 보장
    // 로그로 예외 클래스명과 스택트레이스를 출력해 원인 파악에 활용
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e) {
        log.error("[GlobalExceptionHandler] 처리되지 않은 예외: {} - {}", e.getClass().getName(), e.getMessage(), e);
        String message = "[" + e.getClass().getSimpleName() + "] " + e.getMessage();
        return ResponseEntity.status(500).body(ApiResponse.of(message));
    }
}
