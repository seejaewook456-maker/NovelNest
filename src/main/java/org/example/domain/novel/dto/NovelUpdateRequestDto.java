package org.example.domain.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NovelUpdateRequestDto {

    @Schema(description = "작품 제목", example = "검은 달의 기사 개정판")
    @NotBlank
    private String title;

    @Schema(description = "장르", example = "다크판타지")
    @NotBlank
    private String genre;

    @Schema(description = "작품 소개 (선택)", example = "개정된 작품 소개")
    private String description;
}
