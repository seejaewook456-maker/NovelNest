package org.example.domain.novel.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NovelCreateRequestDto {

    @Schema(description = "작품 제목", example = "검은 달의 기사")
    @NotBlank
    private String title;

    @Schema(description = "장르", example = "판타지")
    @NotBlank
    private String genre;

    @Schema(description = "작품 소개 (선택)", example = "마법이 사라진 세계에서 펼쳐지는 이야기")
    private String description;
}
