package org.example.domain.episodecharacter.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodecharacter.entity.EpisodeCharacter;
import org.example.domain.episodecharacter.repository.EpisodeCharacterRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeCharacterService {

    private final EpisodeCharacterRepository episodeCharacterRepository;
    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;

    // 회차-인물 연결 생성 — 이미 연결된 경우 조용히 무시 (멱등)
    @Transactional
    public void linkCharacter(String email, Long episodeId, Long characterId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        Character character = findCharacterById(characterId);

        if (episodeCharacterRepository.existsByEpisodeAndCharacter_Id(episode, characterId)) {
            return;
        }

        EpisodeCharacter link = EpisodeCharacter.builder()
                .episode(episode)
                .character(character)
                .build();
        episodeCharacterRepository.save(link);
    }

    // 해당 회차에서 추출/저장된 인물 목록 조회
    @Transactional(readOnly = true)
    public List<CharacterResponseDto> getCharactersByEpisode(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        return episodeCharacterRepository.findAllByEpisode(episode).stream()
                .map(ec -> CharacterResponseDto.from(ec.getCharacter()))
                .toList();
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Episode findEpisodeById(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
    }

    private Character findCharacterById(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("등장인물을 찾을 수 없습니다."));
    }

    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 회차에 대한 권한이 없습니다.");
        }
    }
}
