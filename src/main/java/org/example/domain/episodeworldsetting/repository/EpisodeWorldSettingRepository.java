package org.example.domain.episodeworldsetting.repository;

import org.example.domain.episode.entity.Episode;
import org.example.domain.episodeworldsetting.entity.EpisodeWorldSetting;
import org.example.domain.novel.entity.Novel;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeWorldSettingRepository extends JpaRepository<EpisodeWorldSetting, Long> {

    List<EpisodeWorldSetting> findAllByEpisode(Episode episode);

    boolean existsByEpisodeAndWorldSetting_Id(Episode episode, Long worldSettingId);

    void deleteAllByEpisode(Episode episode);

    // 작품 삭제 시 해당 작품의 모든 회차-세계관 연결을 한 번에 삭제
    void deleteAllByEpisode_Novel(Novel novel);

    void deleteAllByWorldSetting(WorldSetting worldSetting);
}
