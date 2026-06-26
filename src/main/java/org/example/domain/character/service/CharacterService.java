package org.example.domain.character.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.character.dto.CharacterCreateRequestDto;
import org.example.domain.character.dto.CharacterFavoriteRequestDto;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.character.dto.CharacterUpdateRequestDto;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.episodecharacter.repository.EpisodeCharacterRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterService {

    private final CharacterRepository characterRepository;
    private final EpisodeCharacterRepository episodeCharacterRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Transactional
    public CharacterResponseDto createCharacter(String email, Long novelId, CharacterCreateRequestDto dto) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        Character character = Character.builder()
                .novel(novel)
                .name(dto.getName())
                .role(dto.getRole())
                .age(dto.getAge())
                .personality(dto.getPersonality())
                .speechStyle(dto.getSpeechStyle())
                .description(dto.getDescription())
                .build();

        Character saved = characterRepository.save(character);
        log.info("Character created. characterId={}, novelId={}, userId={}", saved.getId(), novelId, user.getId());
        return CharacterResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<CharacterResponseDto> getCharacters(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        return characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel).stream()
                .map(CharacterResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CharacterResponseDto getCharacter(String email, Long characterId) {
        User user = findUserByEmail(email);
        Character character = findCharacterById(characterId);
        validateOwner(character.getNovel(), user);

        return CharacterResponseDto.from(character);
    }

    @Transactional
    public CharacterResponseDto updateCharacter(String email, Long characterId, CharacterUpdateRequestDto dto) {
        User user = findUserByEmail(email);
        Character character = findCharacterById(characterId);
        validateOwner(character.getNovel(), user);

        character.update(dto.getName(), dto.getRole(), dto.getAge(),
                dto.getPersonality(), dto.getSpeechStyle(), dto.getDescription());

        return CharacterResponseDto.from(character);
    }

    @Transactional
    public CharacterResponseDto toggleFavorite(String email, Long characterId, CharacterFavoriteRequestDto dto) {
        User user = findUserByEmail(email);
        Character character = findCharacterById(characterId);
        validateOwner(character.getNovel(), user);

        character.updateFavorite(dto.getIsFavorite());
        return CharacterResponseDto.from(character);
    }

    @Transactional
    public void deleteCharacter(String email, Long characterId) {
        User user = findUserByEmail(email);
        Character character = findCharacterById(characterId);
        validateOwner(character.getNovel(), user);

        // 회차-인물 연결 레코드를 먼저 삭제해야 FK 제약 위반을 피할 수 있음
        episodeCharacterRepository.deleteAllByCharacter(character);
        characterRepository.delete(character);
        log.info("Character deleted. characterId={}, userId={}", characterId, user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    private Character findCharacterById(Long characterId) {
        return characterRepository.findById(characterId)
                .orElseThrow(() -> new IllegalArgumentException("등장인물을 찾을 수 없습니다."));
    }

    // character.getNovel()을 통해 소유자 검증 — Character는 Novel을 통해 User에 도달
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
