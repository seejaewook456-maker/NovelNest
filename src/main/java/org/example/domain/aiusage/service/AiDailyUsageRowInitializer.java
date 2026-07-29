package org.example.domain.aiusage.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.aiusage.entity.AiDailyUsage;
import org.example.domain.aiusage.repository.AiDailyUsageRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

// 오늘 날짜의 AiDailyUsage 행이 없으면 0으로 새로 만드는 역할만 담당하는 전용 컴포넌트.
// AiUsageService와 별도의 빈으로 분리하고, 실패(중복키) 시 여기서 잡지 않고 그대로 던지는 이유:
// JPA/Hibernate는 flush 중 제약 위반 등의 예외가 나면 자바 코드에서 그 예외를 catch하더라도
// 내부적으로 트랜잭션을 rollback-only로 표시해버린다("don't flush the Session after an exception
// occurs"). 같은 트랜잭션 안에서 예외를 삼키고 계속 진행하면, 이후 조건부 UPDATE는 실행되더라도
// 커밋 시점에 UnexpectedRollbackException이 난다. 그래서 이 메서드는 예외를 그대로 전파해
// REQUIRES_NEW 트랜잭션이 정상적으로(Spring이) 롤백·종료되도록 하고, 호출부(AiUsageService)가
// 완전히 종료된 이 트랜잭션 밖에서 DataIntegrityViolationException을 잡는다 — 그 시점엔 이미
// 이 메서드의 세션이 깨끗하게 정리된 뒤라, 호출부가 쓰는 별도 세션에는 영향이 없다.
@Component
@RequiredArgsConstructor
class AiDailyUsageRowInitializer {

    private final AiDailyUsageRepository aiDailyUsageRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void ensureRowExists(Long userId, LocalDate usageDate) {
        if (aiDailyUsageRepository.findByUserIdAndUsageDate(userId, usageDate).isPresent()) {
            return;
        }
        aiDailyUsageRepository.saveAndFlush(AiDailyUsage.builder().userId(userId).usageDate(usageDate).build());
    }
}
