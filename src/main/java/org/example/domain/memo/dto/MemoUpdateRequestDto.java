package org.example.domain.memo.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class MemoUpdateRequestDto {

    @Schema(description = "메모 제목", example = "30화 복선 정리 (수정)")
    @NotBlank
    private String title;

    @Schema(description = "메모 내용", example = "수정된 메모 내용")
    @NotBlank
    private String content;
}
