package org.example.domain.memo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class MemoCreateRequestDto {

    @Schema(description = "메모 제목", example = "30화 복선 정리")
    @NotBlank
    private String title;

    @Schema(description = "메모 내용", example = "레온이 3화에서 언급한 반지가 결말의 복선")
    @NotBlank
    private String content;
}
