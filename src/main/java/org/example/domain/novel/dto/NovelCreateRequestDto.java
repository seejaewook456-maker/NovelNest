package org.example.domain.novel.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class NovelCreateRequestDto {

    @NotBlank
    private String title;

    @NotBlank
    private String genre;

    private String description;
}
