package org.example.domain.episodesummary.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.domain.episode.entity.Episode;
import org.example.global.common.BaseEntity;

@Entity
@Table(name = "episode_summaries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EpisodeSummary extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 에피소드당 요약 1개만 허용 — unique = true 로 DB 레벨에서 보장
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false, unique = true)
    private Episode episode;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String summary;

    @Builder
    private EpisodeSummary(Episode episode, String summary) {
        this.episode = episode;
        this.summary = summary;
    }

    // upsert 시 기존 요약 내용을 새 내용으로 교체
    public void updateSummary(String summary) {
        this.summary = summary;
    }
}
