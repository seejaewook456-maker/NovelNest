package org.example.domain.memo.repository;

import org.example.domain.memo.entity.Memo;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MemoRepository extends JpaRepository<Memo, Long> {

    // 즐겨찾기 우선(DESC), 그다음 최근 수정순(DESC) — 메모 관리 페이지/회차 작성 메뉴가
    // 동일한 순서를 쓰도록 정렬을 이 조회 메서드 하나로 통일한다(프론트에서 재정렬하지 않음).
    List<Memo> findAllByNovelOrderByIsFavoriteDescUpdatedAtDesc(Novel novel);

    void deleteAllByNovel(Novel novel);
}
