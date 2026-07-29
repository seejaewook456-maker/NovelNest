package org.example.domain.aiusage.service;

import org.example.domain.aiusage.entity.AiDailyUsage;
import org.example.domain.aiusage.repository.AiDailyUsageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AiDailyUsageRowInitializerTest {

    @Mock
    private AiDailyUsageRepository aiDailyUsageRepository;

    @InjectMocks
    private AiDailyUsageRowInitializer rowInitializer;

    private static final Long USER_ID = 1L;
    private static final LocalDate TODAY = LocalDate.of(2026, 7, 29);

    @Test
    void 행이_없으면_0카운트로_새로_생성한다() {
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY)).willReturn(Optional.empty());

        rowInitializer.ensureRowExists(USER_ID, TODAY);

        ArgumentCaptor<AiDailyUsage> captor = ArgumentCaptor.forClass(AiDailyUsage.class);
        verify(aiDailyUsageRepository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(USER_ID);
        assertThat(captor.getValue().getUsageDate()).isEqualTo(TODAY);
    }

    @Test
    void 행이_이미_있으면_다시_생성하지_않는다() {
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY))
                .willReturn(Optional.of(AiDailyUsage.builder().userId(USER_ID).usageDate(TODAY).build()));

        rowInitializer.ensureRowExists(USER_ID, TODAY);

        verify(aiDailyUsageRepository, never()).saveAndFlush(any());
    }

    @Test
    void 동시_생성으로_인한_중복키_예외는_그대로_전파한다() {
        // 이 메서드 자체는 예외를 삼키지 않는다 — REQUIRES_NEW 트랜잭션이 Spring에 의해 정상적으로
        // 롤백·종료되도록 그대로 던지고, 상위(AiUsageService)가 이 트랜잭션이 완전히 끝난 뒤에 잡는다
        // (같은 트랜잭션 안에서 잡으면 커밋 시 UnexpectedRollbackException이 나기 때문 — 클래스 주석 참고).
        given(aiDailyUsageRepository.findByUserIdAndUsageDate(USER_ID, TODAY)).willReturn(Optional.empty());
        given(aiDailyUsageRepository.saveAndFlush(any())).willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> rowInitializer.ensureRowExists(USER_ID, TODAY))
                .isInstanceOf(DataIntegrityViolationException.class);
    }
}
