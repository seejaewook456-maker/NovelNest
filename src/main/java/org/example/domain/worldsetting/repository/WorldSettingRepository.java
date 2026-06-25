package org.example.domain.worldsetting.repository;

import org.example.domain.novel.entity.Novel;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.entity.WorldSettingCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WorldSettingRepository extends JpaRepository<WorldSetting, Long> {

    // 목록 조회 — 카테고리 오름차순, 제목 오름차순
    List<WorldSetting> findAllByNovelOrderByCategoryAscTitleAsc(Novel novel);

    // 카테고리 필터 — 향후 AI가 특정 카테고리 설정만 조회할 때 사용
    List<WorldSetting> findAllByNovelAndCategoryOrderByTitleAsc(Novel novel, WorldSettingCategory category);

    // 챗봇 통계 전용
    long countByNovel(Novel novel);

    void deleteAllByNovel(Novel novel);
}
