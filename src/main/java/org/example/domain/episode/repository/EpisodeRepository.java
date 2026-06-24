package org.example.domain.episode.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeRepository extends JpaRepository<Episode, Long> {

    List<Episode> findAllByNovelOrderByEpisodeNumberAsc(Novel novel);

    boolean existsByNovelAndEpisodeNumber(Novel novel, int episodeNumber);
}
