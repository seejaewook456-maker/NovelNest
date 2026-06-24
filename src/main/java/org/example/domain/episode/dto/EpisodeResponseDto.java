package org.example.domain.episode.dto;

import lombok.Getter;
import org.example.domain.episode.entity.Episode;

import java.time.LocalDateTime;

@Getter
public class EpisodeResponseDto {

    private final Long id;
    private final Long novelId;
    private final String title;
    private final int episodeNumber;
    private final String content;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private EpisodeResponseDto(Long id, Long novelId, String title, int episodeNumber,
                                String content, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.episodeNumber = episodeNumber;
        this.content = content;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static EpisodeResponseDto from(Episode episode) {
        return new EpisodeResponseDto(
                episode.getId(),
                episode.getNovel().getId(),
                episode.getTitle(),
                episode.getEpisodeNumber(),
                episode.getContent(),
                episode.getCreatedAt(),
                episode.getUpdatedAt()
        );
    }
}
