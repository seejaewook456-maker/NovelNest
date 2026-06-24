package org.example.domain.worldsetting.dto;

import lombok.Getter;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

import java.time.LocalDateTime;

@Getter
public class WorldSettingResponseDto {

    private final Long id;
    private final Long novelId;
    private final WorldSettingCategory category;
    private final String title;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private WorldSettingResponseDto(Long id, Long novelId, WorldSettingCategory category,
                                     String title, String content,
                                     LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.novelId = novelId;
        this.category = category;
        this.title = title;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static WorldSettingResponseDto from(WorldSetting worldSetting) {
        return new WorldSettingResponseDto(
                worldSetting.getId(),
                worldSetting.getNovel().getId(),
                worldSetting.getCategory(),
                worldSetting.getTitle(),
                worldSetting.getContent(),
                worldSetting.getCreatedAt(),
                worldSetting.getUpdatedAt()
        );
    }
}
