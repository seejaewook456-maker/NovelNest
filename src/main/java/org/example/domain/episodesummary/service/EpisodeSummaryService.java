package org.example.domain.episodesummary.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodesummary.dto.EpisodeSummaryResponseDto;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.ai.service.OpenAiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class EpisodeSummaryService {

    // AI에게 전달하는 역할 지시문 — 소설 편집자로서 3~5문장 요약을 요청
    private static final String SUMMARY_INSTRUCTIONS =
            "당신은 소설 편집 전문가입니다. 주어진 소설 회차 내용을 3~5문장으로 간결하게 요약해주세요. " +
            "핵심 사건, 등장인물의 주요 행동, 감정 변화를 중심으로 요약하세요.";

    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final EpisodeRepository episodeRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;

    // 요약 생성 (없으면 insert, 있으면 update — upsert)
    @Transactional
    public EpisodeSummaryResponseDto generateSummary(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        String summaryText = openAiService.generateText(SUMMARY_INSTRUCTIONS, episode.getContent());

        // 기존 요약이 있으면 덮어쓰고, 없으면 새로 저장
        Optional<EpisodeSummary> existing = episodeSummaryRepository.findByEpisode(episode);
        EpisodeSummary episodeSummary;
        if (existing.isPresent()) {
            episodeSummary = existing.get();
            episodeSummary.updateSummary(summaryText);
        } else {
            episodeSummary = episodeSummaryRepository.save(
                    EpisodeSummary.builder()
                            .episode(episode)
                            .summary(summaryText)
                            .build()
            );
        }

        log.info("Episode summary generated. episodeId={}, userId={}", episodeId, user.getId());
        return EpisodeSummaryResponseDto.from(episodeSummary);
    }

    @Transactional(readOnly = true)
    public EpisodeSummaryResponseDto getSummary(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        EpisodeSummary episodeSummary = episodeSummaryRepository.findByEpisode(episode)
                .orElseThrow(() -> new IllegalArgumentException("아직 생성된 요약이 없습니다."));

        return EpisodeSummaryResponseDto.from(episodeSummary);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Episode findEpisodeById(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
    }

    // episode → novel → user 체인으로 소유자 검증
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 회차에 대한 권한이 없습니다.");
        }
    }
}
