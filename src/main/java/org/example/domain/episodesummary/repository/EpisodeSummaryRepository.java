package org.example.domain.episodesummary.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EpisodeSummaryRepository extends JpaRepository<EpisodeSummary, Long> {

    Optional<EpisodeSummary> findByEpisode(Episode episode);

    // 충돌 탐지 시 해당 작품의 최근 10개 요약을 회차 번호 내림차순으로 조회
    List<EpisodeSummary> findTop10ByEpisode_NovelOrderByEpisode_EpisodeNumberDesc(Novel novel);

    // 작품 삭제 시 해당 작품의 모든 요약을 한 번에 삭제
    void deleteAllByEpisode_Novel(Novel novel);
}
