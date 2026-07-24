package org.example.domain.episodesummary.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EpisodeSummaryRepository extends JpaRepository<EpisodeSummary, Long> {

    Optional<EpisodeSummary> findByEpisode(Episode episode);

    // 충돌 탐지 시 해당 작품의 최근 N개 요약을 회차 번호 내림차순으로 조회
    // — 파생 메서드 대신 명시적 JPQL 사용: @OneToOne(LAZY) + 중첩 속성 탐색 조합 시
    //   Hibernate 런타임 오류를 방지하기 위함
    @Query("SELECT es FROM EpisodeSummary es JOIN es.episode e WHERE e.novel = :novel ORDER BY e.episodeNumber DESC")
    List<EpisodeSummary> findRecentSummariesByNovel(@Param("novel") Novel novel, Pageable pageable);

    // 충돌 탐지 시 검사 대상 회차보다 이전 회차의 최근 N개 요약만 DB 단계에서 조회
    // (전체를 가져온 뒤 Java에서 필터링하면 대상 회차가 최신이 아닐 때 결과 개수가 부정확해짐)
    @Query("SELECT es FROM EpisodeSummary es JOIN es.episode e " +
            "WHERE e.novel = :novel AND e.episodeNumber < :beforeEpisodeNumber ORDER BY e.episodeNumber DESC")
    List<EpisodeSummary> findRecentSummariesBeforeEpisode(@Param("novel") Novel novel,
                                                            @Param("beforeEpisodeNumber") int beforeEpisodeNumber,
                                                            Pageable pageable);

    // 챗봇 통계 전용 — 전체 로드 없이 카운트만 조회
    @Query("SELECT COUNT(es) FROM EpisodeSummary es JOIN es.episode e WHERE e.novel = :novel")
    long countSummariesByNovel(@Param("novel") Novel novel);

    // 작품 삭제 시 해당 작품의 모든 요약을 한 번에 삭제
    void deleteAllByEpisode_Novel(Novel novel);
}
