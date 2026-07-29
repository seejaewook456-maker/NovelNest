package org.example.domain.aiusage.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.global.common.BaseEntity;

import java.time.LocalDate;

// 사용자별·날짜별 AI 기능 하루 사용량 카운터.
// user와의 관계를 JPA @ManyToOne으로 매핑하지 않고 userId(순수 컬럼)로만 저장한다 — 이 엔티티는
// 항상 userId+usageDate로만 조회/증가되고 User 쪽으로 객체 그래프 탐색이 필요 없는 카운터 성격의
// 테이블이라, 불필요한 연관관계 결합(및 트랜잭션 경계를 넘나드는 detached 엔티티 이슈)을 피하기 위함이다.
// 참조 무결성은 DB의 FK 제약(Flyway V6)으로 보장한다.
//
// "매일 초기화"는 별도 배치로 기존 행을 0으로 되돌리는 방식이 아니라, (user_id, usage_date) 유니크
// 제약 덕분에 날짜가 바뀌면 자연스럽게 새 행이 생성되는 방식으로 구현한다 — 지난 날짜의 행은 그대로
// 이력으로 남는다.
@Entity
@Table(
        name = "ai_daily_usages",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_ai_daily_usages_user_date", columnNames = {"user_id", "usage_date"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiDailyUsage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;

    @Column(name = "summary_count", nullable = false)
    private int summaryCount;

    @Column(name = "conflict_count", nullable = false)
    private int conflictCount;

    @Column(name = "character_count", nullable = false)
    private int characterCount;

    @Column(name = "worldview_count", nullable = false)
    private int worldviewCount;

    @Column(name = "chat_count", nullable = false)
    private int chatCount;

    @Builder
    private AiDailyUsage(Long userId, LocalDate usageDate) {
        this.userId = userId;
        this.usageDate = usageDate;
    }
}
