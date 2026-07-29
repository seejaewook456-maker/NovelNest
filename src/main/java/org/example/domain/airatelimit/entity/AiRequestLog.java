package org.example.domain.airatelimit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 분당 Rate Limit(최근 60초 Sliding Window) 판단을 위한 사용자별 AI 요청 시각 로그.
// Sliding Window 계산에만 쓰이는 휘발성 데이터라 BaseEntity(createdAt/updatedAt)를 상속하지 않고
// requestedAt 하나만 둔다 — 이 행은 생성된 뒤 절대 수정되지 않고, 윈도우 밖으로 나가면 삭제만 된다.
@Entity
@Table(name = "ai_request_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiRequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    public AiRequestLog(Long userId, LocalDateTime requestedAt) {
        this.userId = userId;
        this.requestedAt = requestedAt;
    }
}
