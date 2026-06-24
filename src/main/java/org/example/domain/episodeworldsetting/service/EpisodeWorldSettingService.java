package org.example.domain.episodeworldsetting.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodeworldsetting.entity.EpisodeWorldSetting;
import org.example.domain.episodeworldsetting.repository.EpisodeWorldSettingRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class EpisodeWorldSettingService {

    private final EpisodeWorldSettingRepository episodeWorldSettingRepository;
    private final EpisodeRepository episodeRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final UserRepository userRepository;

    // 회차-세계관 연결 생성 — 이미 연결된 경우 조용히 무시 (멱등)
    @Transactional
    public void linkWorldSetting(String email, Long episodeId, Long worldSettingId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        WorldSetting worldSetting = findWorldSettingById(worldSettingId);

        if (episodeWorldSettingRepository.existsByEpisodeAndWorldSetting_Id(episode, worldSettingId)) {
            return;
        }

        EpisodeWorldSetting link = EpisodeWorldSetting.builder()
                .episode(episode)
                .worldSetting(worldSetting)
                .build();
        episodeWorldSettingRepository.save(link);
    }

    // 해당 회차에서 추출/저장된 세계관 설정 목록 조회
    @Transactional(readOnly = true)
    public List<WorldSettingResponseDto> getWorldSettingsByEpisode(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        return episodeWorldSettingRepository.findAllByEpisode(episode).stream()
                .map(ews -> WorldSettingResponseDto.from(ews.getWorldSetting()))
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

    private WorldSetting findWorldSettingById(Long worldSettingId) {
        return worldSettingRepository.findById(worldSettingId)
                .orElseThrow(() -> new IllegalArgumentException("세계관 설정을 찾을 수 없습니다."));
    }

    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 회차에 대한 권한이 없습니다.");
        }
    }
}
