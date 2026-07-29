package org.example.global.exception;

import io.sentry.Sentry;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.example.global.common.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.BindException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.List;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // BusinessException — ErrorCode 기반 커스텀 예외
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse> handleBusinessException(BusinessException e) {
        ErrorCode errorCode = e.getErrorCode();
        log.warn("[BusinessException] code={}, message={}", errorCode.getCode(), e.getMessage());
        return ResponseEntity.status(errorCode.getStatus())
                .body(ApiResponse.fail(errorCode.getCode(), e.getMessage()));
    }

    // @Valid / @Validated 검증 실패 — 필드별 에러 목록 반환
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(MethodArgumentNotValidException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", defaultMessage(error)))
                .toList();
        log.warn("[Validation] errors={}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        ErrorCode.VALIDATION_ERROR.getMessage(),
                        Map.of("errors", errors)
                ));
    }

    // @ModelAttribute 등 바인딩 오류 (MethodArgumentNotValidException의 부모)
    @ExceptionHandler(BindException.class)
    public ResponseEntity<ApiResponse> handleBindException(BindException e) {
        List<Map<String, String>> errors = e.getBindingResult().getFieldErrors().stream()
                .map(error -> Map.of("field", error.getField(), "message", defaultMessage(error)))
                .toList();
        log.warn("[BindException] errors={}", errors);
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(
                        ErrorCode.VALIDATION_ERROR.getCode(),
                        ErrorCode.VALIDATION_ERROR.getMessage(),
                        Map.of("errors", errors)
                ));
    }

    // 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse> handleMissingServletRequestParameterException(MissingServletRequestParameterException e) {
        log.warn("[MissingParameter] parameter={}", e.getParameterName());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.MISSING_PARAMETER.getCode(), e.getMessage()));
    }

    // 잘못된 JSON / Enum 형식
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException e) {
        log.warn("[InvalidFormat] message={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_FORMAT.getCode(), ErrorCode.INVALID_FORMAT.getMessage()));
    }

    // 존재하지 않는 URL (Spring 6.x: NoResourceFoundException)
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiResponse> handleNoResourceFoundException(NoResourceFoundException e) {
        log.warn("[NotFound] method={}, url={}", e.getHttpMethod(), e.getResourcePath());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    // 존재하지 않는 URL (레거시 핸들러)
    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse> handleNoHandlerFoundException(NoHandlerFoundException e) {
        log.warn("[NotFound] method={}, url={}", e.getHttpMethod(), e.getRequestURL());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ApiResponse.fail(ErrorCode.NOT_FOUND.getCode(), ErrorCode.NOT_FOUND.getMessage()));
    }

    // Spring Security 권한 부족 (403)
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse> handleAccessDeniedException(AccessDeniedException e) {
        log.warn("[AccessDenied] message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN.getCode(), ErrorCode.FORBIDDEN.getMessage()));
    }

    // Spring Security 인증 실패 (401)
    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ApiResponse> handleAuthenticationException(AuthenticationException e) {
        log.warn("[Unauthorized] message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiResponse.fail(ErrorCode.UNAUTHORIZED.getCode(), ErrorCode.UNAUTHORIZED.getMessage()));
    }

    // 파일 크기 초과 (MaxUploadSizeExceededException이 MultipartException의 하위 클래스이므로 먼저 선언)
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException e) {
        log.warn("[FileTooLarge] message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(ApiResponse.fail(ErrorCode.FILE_TOO_LARGE.getCode(), ErrorCode.FILE_TOO_LARGE.getMessage()));
    }

    // 파일 업로드 오류
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ApiResponse> handleMultipartException(MultipartException e) {
        log.warn("[Multipart] message={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.MULTIPART_ERROR.getCode(), ErrorCode.MULTIPART_ERROR.getMessage()));
    }

    // 비즈니스 예외 (기존 서비스 코드 호환 — IllegalArgumentException)
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse> handleIllegalArgumentException(IllegalArgumentException e) {
        log.warn("[IllegalArgument] message={}", e.getMessage());
        return ResponseEntity.badRequest()
                .body(ApiResponse.fail(ErrorCode.INVALID_INPUT.getCode(), e.getMessage()));
    }

    // 리소스 소유권 오류 (기존 서비스 코드 호환 — java.lang.SecurityException)
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse> handleSecurityException(SecurityException e) {
        log.warn("[SecurityException] message={}", e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiResponse.fail(ErrorCode.FORBIDDEN.getCode(), e.getMessage()));
    }

    // 처리되지 않은 모든 예외 — 500.
    // 이 핸들러가 Exception 전체를 잡아버려 Sentry의 자동 예외 리졸버(HandlerExceptionResolver)까지
    // 도달하는 예외가 없으므로, 여기서만 한 번 수동으로 Sentry에 전달한다.
    // BusinessException 등 더 구체적인 타입은 위쪽의 전용 핸들러에서 먼저 잡혀 여기 도달하지 않으므로
    // 중복 전송되지 않는다. 요청 Body/쿠키/인증 헤더는 담지 않는다(sentry.max-request-body-size=none).
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse> handleException(Exception e, HttpServletRequest request) {
        log.error("[ServerError] method={}, uri={}, exception={}, message={}",
                request.getMethod(), request.getRequestURI(), e.getClass().getName(), e.getMessage(), e);
        Sentry.captureException(e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR.getCode(), ErrorCode.INTERNAL_SERVER_ERROR.getMessage()));
    }

    private String defaultMessage(FieldError error) {
        String msg = error.getDefaultMessage();
        return msg != null ? msg : "올바르지 않은 값입니다.";
    }
}
