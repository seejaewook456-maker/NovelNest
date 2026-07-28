package org.example.domain.user.policy;

import lombok.RequiredArgsConstructor;
import org.example.domain.user.config.RejoinPolicyProperties;
import org.example.domain.user.entity.User;
import org.example.global.exception.BusinessException;
import org.example.global.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Optional;

// 이메일 회원가입과 OAuth 회원가입 양쪽에서 공통으로 사용하는 재가입 제한 정책.
// "활성 회원인지 확인 → 탈퇴 회원인지 확인 → 탈퇴 후 설정된 기간이 지났는지 확인" 순서 중
// 뒤의 두 단계(탈퇴 이력 확인 + 기간 경과 확인)를 이 클래스가 전담한다.
// 활성 회원 존재 여부는 호출부마다 던지는 중복 에러코드가 다르므로(EMAIL_ALREADY_REGISTERED,
// email_already_exists 등) 이 클래스에서 처리하지 않고 호출부에 남긴다.
@Component
@RequiredArgsConstructor
public class UserRejoinPolicy {

    private final RejoinPolicyProperties rejoinPolicyProperties;

    public void assertRejoinAllowed(Optional<User> latestWithdrawnUser) {
        latestWithdrawnUser.ifPresent(user -> {
            int blockDays = rejoinPolicyProperties.getBlockDays();
            LocalDateTime blockedUntil = user.getDeletedAt().plusDays(blockDays);
            if (LocalDateTime.now().isBefore(blockedUntil)) {
                throw new BusinessException(
                        ErrorCode.USER_REJOIN_BLOCKED,
                        "회원 탈퇴 후 " + blockDays + "일이 지나야 다시 가입할 수 있습니다."
                );
            }
        });
    }
}
