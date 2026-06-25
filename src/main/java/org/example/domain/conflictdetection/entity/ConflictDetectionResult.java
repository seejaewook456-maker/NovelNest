package org.example.domain.conflictdetection.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.episode.entity.Episode;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "conflict_detection_results")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ConflictDetectionResult extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 회차당 결과 1개 — EpisodeSummary와 동일한 OneToOne upsert 패턴
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false, unique = true)
    private Episode episode;

    // 충돌 목록 전체를 JSON 배열로 직렬화해 저장 — 항상 통째로 교체하므로 별도 테이블 불필요
    @Column(nullable = false, columnDefinition = "TEXT")
    private String conflictsJson;

    @Column(nullable = false)
    private int conflictCount;

    @Builder
    private ConflictDetectionResult(Episode episode, String conflictsJson, int conflictCount) {
        this.episode = episode;
        this.conflictsJson = conflictsJson;
        this.conflictCount = conflictCount;
    }

    public void update(String conflictsJson, int conflictCount) {
        this.conflictsJson = conflictsJson;
        this.conflictCount = conflictCount;
    }
}
