package org.example.domain.conflictdetection.repository;

import org.example.domain.conflictdetection.entity.ConflictDetectionResult;
import org.example.domain.episode.entity.Episode;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ConflictDetectionResultRepository extends JpaRepository<ConflictDetectionResult, Long> {

    Optional<ConflictDetectionResult> findByEpisode(Episode episode);

    // 회차 삭제 시 해당 분석 결과 함께 삭제
    void deleteByEpisode(Episode episode);

    // 소설 삭제 시 해당 소설의 모든 분석 결과 함께 삭제
    void deleteAllByEpisode_Novel(Novel novel);
}
