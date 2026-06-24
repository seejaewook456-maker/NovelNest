package org.example.domain.episodecharacter.repository;

import org.example.domain.character.entity.Character;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episodecharacter.entity.EpisodeCharacter;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EpisodeCharacterRepository extends JpaRepository<EpisodeCharacter, Long> {

    List<EpisodeCharacter> findAllByEpisode(Episode episode);

    boolean existsByEpisodeAndCharacter_Id(Episode episode, Long characterId);

    void deleteAllByEpisode(Episode episode);

    void deleteAllByCharacter(Character character);
}
