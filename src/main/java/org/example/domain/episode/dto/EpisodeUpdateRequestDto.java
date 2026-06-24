package org.example.domain.episode.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class EpisodeUpdateRequestDto {

    @Schema(description = "회차 제목", example = "시작 (개정)")
    @NotBlank
    private String title;

    @Schema(description = "회차 번호 (1 이상)", example = "1")
    @NotNull
    @Min(1)
    private Integer episodeNumber;

    @Schema(description = "회차 내용", example = "개정된 1화 내용.")
    @NotBlank
    private String content;
}
