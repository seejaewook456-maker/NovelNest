package org.example.domain.memo.dto;

import lombok.Getter;
import org.example.domain.memo.entity.Memo;

import java.time.LocalDateTime;

@Getter
public class MemoResponseDto {

    private final Long id;
    private final Long novelId;
    private final String title;
    private final String content;
    private final Boolean isFavorite;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private MemoResponseDto(Long id, Long novelId, String title, String content, Boolean isFavorite,
                             LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.novelId = novelId;
        this.title = title;
        this.content = content;
        this.isFavorite = isFavorite;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static MemoResponseDto from(Memo memo) {
        return new MemoResponseDto(
                memo.getId(),
                memo.getNovel().getId(),
                memo.getTitle(),
                memo.getContent(),
                memo.getIsFavorite(),
                memo.getCreatedAt(),
                memo.getUpdatedAt()
        );
    }
}
