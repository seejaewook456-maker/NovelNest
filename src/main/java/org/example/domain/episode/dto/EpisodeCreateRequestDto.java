package org.example.domain.episode.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class EpisodeCreateRequestDto {

    @NotBlank
    private String title;

    @NotNull
    @Min(1)
    private Integer episodeNumber;

    @NotBlank
    private String content;
}
