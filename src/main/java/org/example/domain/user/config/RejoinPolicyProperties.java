package org.example.domain.user.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

// 회원 탈퇴 후 재가입 제한 기간을 설정값으로 관리한다(application.yml의 app.user.rejoin.block-days).
// 하드코딩 대신 이 클래스를 통해 주입받으면, 정책 기간을 바꿀 때 코드 수정 없이 설정만 바꾸면 된다.
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.user.rejoin")
public class RejoinPolicyProperties {

    private int blockDays = 14;
}
