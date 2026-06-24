package org.example.domain.episodesummary.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EpisodeSummaryRepository extends JpaRepository<EpisodeSummary, Long> {

    Optional<EpisodeSummary> findByEpisode(Episode episode);
}
