package org.example.domain.episode.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findAllByNovelOrderByEpisodeNumberAsc(Novel novel);

    boolean existsByNovelAndEpisodeNumber(Novel novel, int episodeNumber);

    // 챗봇 통계 전용 — 전체 로드 없이 카운트만 조회
    long countByNovel(Novel novel);

    void deleteAllByNovel(Novel novel);
}
