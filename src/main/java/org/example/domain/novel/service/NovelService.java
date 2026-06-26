package org.example.domain.novel.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.conflictdetection.repository.ConflictDetectionResultRepository;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodecharacter.repository.EpisodeCharacterRepository;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.episodeworldsetting.repository.EpisodeWorldSettingRepository;
import org.example.domain.novel.dto.NovelCreateRequestDto;
import org.example.domain.novel.dto.NovelResponseDto;
import org.example.domain.novel.dto.NovelUpdateRequestDto;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NovelService {

    private final NovelRepository novelRepository;
    private final EpisodeCharacterRepository episodeCharacterRepository;
    private final EpisodeWorldSettingRepository episodeWorldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final ConflictDetectionResultRepository conflictDetectionResultRepository;
    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final UserRepository userRepository;

    @Transactional
    public NovelResponseDto createNovel(String email, NovelCreateRequestDto dto) {
        User user = findUserByEmail(email);

        Novel novel = Novel.builder()
                .user(user)
                .title(dto.getTitle())
                .genre(dto.getGenre())
                .description(dto.getDescription())
                .build();

        Novel saved = novelRepository.save(novel);
        log.info("Novel created. novelId={}, userId={}", saved.getId(), user.getId());
        return NovelResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<NovelResponseDto> getMyNovels(String email) {
        User user = findUserByEmail(email);
        return novelRepository.findAllByUser(user).stream()
                .map(NovelResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public NovelResponseDto getNovel(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);
        return NovelResponseDto.from(novel);
    }

    @Transactional
    public NovelResponseDto updateNovel(String email, Long novelId, NovelUpdateRequestDto dto) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        novel.update(dto.getTitle(), dto.getGenre(), dto.getDescription());
        log.info("Novel updated. novelId={}, userId={}", novelId, user.getId());
        return NovelResponseDto.from(novel);
    }

    @Transactional
    public void deleteNovel(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        // FK 제약 순서에 맞춰 하위 엔티티를 먼저 삭제
        episodeCharacterRepository.deleteAllByEpisode_Novel(novel);
        episodeWorldSettingRepository.deleteAllByEpisode_Novel(novel);
        episodeSummaryRepository.deleteAllByEpisode_Novel(novel);
        conflictDetectionResultRepository.deleteAllByEpisode_Novel(novel);
        episodeRepository.deleteAllByNovel(novel);
        characterRepository.deleteAllByNovel(novel);
        worldSettingRepository.deleteAllByNovel(novel);
        novelRepository.delete(novel);
        log.info("Novel deleted. novelId={}, userId={}", novelId, user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    // 요청자와 작품 소유자가 다르면 접근 거부
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
