package org.example.domain.episode.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.episode.dto.EpisodeCreateRequestDto;
import org.example.domain.episode.dto.EpisodeResponseDto;
import org.example.domain.episode.dto.EpisodeUpdateRequestDto;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.conflictdetection.repository.ConflictDetectionResultRepository;
import org.example.domain.episodecharacter.repository.EpisodeCharacterRepository;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.episodeworldsetting.repository.EpisodeWorldSettingRepository;
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
public class EpisodeService {

    private final EpisodeRepository episodeRepository;
    private final EpisodeCharacterRepository episodeCharacterRepository;
    private final EpisodeWorldSettingRepository episodeWorldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final ConflictDetectionResultRepository conflictDetectionResultRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Transactional
    public EpisodeResponseDto createEpisode(String email, Long novelId, EpisodeCreateRequestDto dto) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        if (episodeRepository.existsByNovelAndEpisodeNumber(novel, dto.getEpisodeNumber())) {
            throw new IllegalArgumentException(dto.getEpisodeNumber() + "화는 이미 존재합니다.");
        }

        Episode episode = Episode.builder()
                .novel(novel)
                .title(dto.getTitle())
                .episodeNumber(dto.getEpisodeNumber())
                .content(dto.getContent())
                .build();

        Episode saved = episodeRepository.save(episode);
        log.info("Episode created. episodeId={}, novelId={}, userId={}", saved.getId(), novelId, user.getId());
        return EpisodeResponseDto.from(saved);
    }

    @Transactional(readOnly = true)
    public List<EpisodeResponseDto> getEpisodes(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        return episodeRepository.findAllByNovelOrderByEpisodeNumberAsc(novel).stream()
                .map(EpisodeResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public EpisodeResponseDto getEpisode(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        return EpisodeResponseDto.from(episode);
    }

    @Transactional
    public EpisodeResponseDto updateEpisode(String email, Long episodeId, EpisodeUpdateRequestDto dto) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        // 회차 번호가 변경되는 경우에만 중복 체크
        if (episode.getEpisodeNumber() != dto.getEpisodeNumber()
                && episodeRepository.existsByNovelAndEpisodeNumber(episode.getNovel(), dto.getEpisodeNumber())) {
            throw new IllegalArgumentException(dto.getEpisodeNumber() + "화는 이미 존재합니다.");
        }

        episode.update(dto.getTitle(), dto.getEpisodeNumber(), dto.getContent());
        log.info("Episode updated. episodeId={}, userId={}", episodeId, user.getId());
        return EpisodeResponseDto.from(episode);
    }

    @Transactional
    public void deleteEpisode(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        // 삭제 순서: EpisodeCharacter → EpisodeWorldSetting → EpisodeSummary → ConflictDetectionResult → Episode
        episodeCharacterRepository.deleteAllByEpisode(episode);
        episodeWorldSettingRepository.deleteAllByEpisode(episode);
        episodeSummaryRepository.findByEpisode(episode)
                .ifPresent(episodeSummaryRepository::delete);
        conflictDetectionResultRepository.findByEpisode(episode)
                .ifPresent(conflictDetectionResultRepository::delete);
        episodeRepository.delete(episode);
        log.info("Episode deleted. episodeId={}, userId={}", episodeId, user.getId());
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    private Episode findEpisodeById(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
    }

    // novel.getUser()를 통해 소유자 검증 — Episode는 Novel을 통해 User에 도달
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
