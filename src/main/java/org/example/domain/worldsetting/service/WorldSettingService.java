package org.example.domain.worldsetting.service;

import lombok.RequiredArgsConstructor;
import org.example.domain.novel.entity.Novel;
import org.example.domain.novel.repository.NovelRepository;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.dto.WorldSettingCreateRequestDto;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.dto.WorldSettingUpdateRequestDto;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WorldSettingService {

    private final WorldSettingRepository worldSettingRepository;
    private final NovelRepository novelRepository;
    private final UserRepository userRepository;

    @Transactional
    public WorldSettingResponseDto createWorldSetting(String email, Long novelId, WorldSettingCreateRequestDto dto) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        WorldSetting worldSetting = WorldSetting.builder()
                .novel(novel)
                .category(dto.getCategory())
                .title(dto.getTitle())
                .content(dto.getContent())
                .build();

        return WorldSettingResponseDto.from(worldSettingRepository.save(worldSetting));
    }

    @Transactional(readOnly = true)
    public List<WorldSettingResponseDto> getWorldSettings(String email, Long novelId) {
        User user = findUserByEmail(email);
        Novel novel = findNovelById(novelId);
        validateOwner(novel, user);

        return worldSettingRepository.findAllByNovelOrderByCategoryAscTitleAsc(novel).stream()
                .map(WorldSettingResponseDto::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public WorldSettingResponseDto getWorldSetting(String email, Long worldSettingId) {
        User user = findUserByEmail(email);
        WorldSetting worldSetting = findWorldSettingById(worldSettingId);
        validateOwner(worldSetting.getNovel(), user);

        return WorldSettingResponseDto.from(worldSetting);
    }

    @Transactional
    public WorldSettingResponseDto updateWorldSetting(String email, Long worldSettingId, WorldSettingUpdateRequestDto dto) {
        User user = findUserByEmail(email);
        WorldSetting worldSetting = findWorldSettingById(worldSettingId);
        validateOwner(worldSetting.getNovel(), user);

        worldSetting.update(dto.getCategory(), dto.getTitle(), dto.getContent());
        return WorldSettingResponseDto.from(worldSetting);
    }

    @Transactional
    public void deleteWorldSetting(String email, Long worldSettingId) {
        User user = findUserByEmail(email);
        WorldSetting worldSetting = findWorldSettingById(worldSettingId);
        validateOwner(worldSetting.getNovel(), user);

        worldSettingRepository.delete(worldSetting);
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Novel findNovelById(Long novelId) {
        return novelRepository.findById(novelId)
                .orElseThrow(() -> new IllegalArgumentException("작품을 찾을 수 없습니다."));
    }

    private WorldSetting findWorldSettingById(Long worldSettingId) {
        return worldSettingRepository.findById(worldSettingId)
                .orElseThrow(() -> new IllegalArgumentException("세계관 설정을 찾을 수 없습니다."));
    }

    // worldSetting.getNovel()을 통해 소유자 검증 — WorldSetting은 Novel을 통해 User에 도달
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 작품에 대한 권한이 없습니다.");
        }
    }
}
