package org.example.domain.character.repository;

import org.example.domain.character.entity.Character;
import org.example.domain.novel.entity.Novel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CharacterRepository extends JpaRepository<Character, Long> {

    List<Character> findAllByNovelOrderByNameAsc(Novel novel);

    // 챗봇 통계 전용
    long countByNovel(Novel novel);

    void deleteAllByNovel(Novel novel);
}
