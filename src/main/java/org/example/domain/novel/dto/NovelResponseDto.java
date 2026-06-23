package org.example.domain.novel.dto;

import lombok.Getter;
import org.example.domain.novel.entity.Novel;

import java.time.LocalDateTime;

@Getter
public class NovelResponseDto {

    private final Long id;
    private final Long userId;
    private final String title;
    private final String genre;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private NovelResponseDto(Long id, Long userId, String title, String genre,
                              String description, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.userId = userId;
        this.title = title;
        this.genre = genre;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static NovelResponseDto from(Novel novel) {
        return new NovelResponseDto(
                novel.getId(),
                novel.getUser().getId(),
                novel.getTitle(),
                novel.getGenre(),
                novel.getDescription(),
                novel.getCreatedAt(),
                novel.getUpdatedAt()
        );
    }
}
