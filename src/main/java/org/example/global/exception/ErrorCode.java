package org.example.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // 인증/인가
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),

    // 요청 오류
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "입력값이 올바르지 않습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    INVALID_FORMAT(HttpStatus.BAD_REQUEST, "INVALID_FORMAT", "입력값 형식이 올바르지 않습니다."),
    MISSING_PARAMETER(HttpStatus.BAD_REQUEST, "MISSING_PARAMETER", "필수 파라미터가 누락되었습니다."),

    // 리소스
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 리소스를 찾을 수 없습니다."),
    CONFLICT(HttpStatus.CONFLICT, "CONFLICT", "이미 존재하는 리소스입니다."),

    // 파일
    FILE_TOO_LARGE(HttpStatus.PAYLOAD_TOO_LARGE, "FILE_TOO_LARGE", "파일 크기가 허용 한도를 초과했습니다."),
    MULTIPART_ERROR(HttpStatus.BAD_REQUEST, "MULTIPART_ERROR", "파일 업로드 중 오류가 발생했습니다."),

    // 회원가입
    PASSWORD_CONFIRM_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_CONFIRM_MISMATCH", "비밀번호와 비밀번호 확인이 일치하지 않습니다."),

    // 이메일 인증
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "EMAIL_ALREADY_REGISTERED", "이미 가입된 이메일입니다."),
    EMAIL_CODE_RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "EMAIL_CODE_RESEND_TOO_SOON", "인증번호는 60초 이후에 재전송할 수 있습니다."),
    EMAIL_SEND_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "EMAIL_SEND_FAILED", "인증 이메일 발송에 실패했습니다."),
    EMAIL_VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "EMAIL_VERIFICATION_NOT_FOUND", "인증번호 발송 이력이 없습니다. 인증번호를 다시 요청해주세요."),
    EMAIL_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "EMAIL_CODE_MISMATCH", "인증번호가 일치하지 않습니다."),
    EMAIL_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "EMAIL_CODE_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    EMAIL_NOT_VERIFIED(HttpStatus.BAD_REQUEST, "EMAIL_NOT_VERIFIED", "이메일 인증이 완료되지 않았습니다."),

    // 비밀번호 재설정
    // 소셜 로그인 전용 계정의 비밀번호 재설정 시도 — 계정 열거 방지를 위해 API 응답에는 노출하지 않고 내부 로직 분기에만 사용
    SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "SOCIAL_ACCOUNT_PASSWORD_RESET_NOT_ALLOWED", "소셜 로그인 계정은 비밀번호 재설정을 이용할 수 없습니다."),
    PASSWORD_RESET_CODE_NOT_FOUND(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_CODE_NOT_FOUND", "인증번호 발송 이력이 없습니다. 인증번호를 다시 요청해주세요."),
    PASSWORD_RESET_CODE_MISMATCH(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_CODE_MISMATCH", "인증번호가 일치하지 않습니다."),
    PASSWORD_RESET_CODE_EXPIRED(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_CODE_EXPIRED", "인증번호가 만료되었습니다. 다시 요청해주세요."),
    PASSWORD_RESET_TOKEN_INVALID(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_INVALID", "유효하지 않은 재설정 요청입니다. 처음부터 다시 시도해주세요."),
    PASSWORD_RESET_TOKEN_EXPIRED(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_EXPIRED", "재설정 가능 시간이 만료되었습니다. 처음부터 다시 시도해주세요."),
    PASSWORD_RESET_TOKEN_ALREADY_USED(HttpStatus.BAD_REQUEST, "PASSWORD_RESET_TOKEN_ALREADY_USED", "이미 처리된 재설정 요청입니다. 처음부터 다시 시도해주세요."),

    // Refresh Token
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_NOT_FOUND", "Refresh Token이 존재하지 않습니다."),
    REFRESH_TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_EXPIRED", "Refresh Token이 만료되었습니다. 다시 로그인해주세요."),
    INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "INVALID_REFRESH_TOKEN", "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_MISMATCH(HttpStatus.UNAUTHORIZED, "REFRESH_TOKEN_MISMATCH", "저장된 Refresh Token과 일치하지 않습니다."),
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "사용자를 찾을 수 없습니다."),

    // 회원 탈퇴
    USER_ALREADY_WITHDRAWN(HttpStatus.CONFLICT, "USER_ALREADY_WITHDRAWN", "이미 탈퇴한 사용자입니다."),
    WITHDRAWN_USER(HttpStatus.UNAUTHORIZED, "WITHDRAWN_USER", "탈퇴한 계정입니다. 다시 로그인해주세요."),
    // 이메일 로그인 시, 활성 회원은 아니지만 탈퇴 이력이 있는 이메일로 로그인을 시도한 경우 전용 안내
    WITHDRAWN_ACCOUNT_LOGIN(HttpStatus.BAD_REQUEST, "WITHDRAWN_ACCOUNT_LOGIN", "회원 탈퇴 처리된 이메일입니다."),
    // 탈퇴 후 재가입 제한 기간(app.user.rejoin.block-days)이 지나지 않은 계정으로 재가입을 시도한 경우.
    // 실제 발생 시에는 BusinessException(ErrorCode, 커스텀 메시지)로 설정된 기간(N일)을 담은 메시지를 사용하며,
    // 아래 기본 메시지는 설정값을 알 수 없는 상황을 위한 fallback이다.
    USER_REJOIN_BLOCKED(HttpStatus.CONFLICT, "USER_REJOIN_BLOCKED", "회원 탈퇴 후 14일이 지나야 다시 가입할 수 있습니다."),

    // AI 하루 사용량 제한 — 기본 메시지는 fallback이며, 실제로는 기능명을 담은 커스텀 메시지로 던진다
    // (AiUsageService 참고). EMAIL_CODE_RESEND_TOO_SOON과 동일하게 429 Too Many Requests를 사용한다.
    AI_DAILY_FEATURE_LIMIT_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "AI_DAILY_FEATURE_LIMIT_EXCEEDED",
            "오늘 사용할 수 있는 AI 기능 이용 횟수를 모두 사용했습니다. AI 도구 이용권은 매일 00:00에 충전됩니다."),

    // 서버 오류
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "서버 오류가 발생했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
