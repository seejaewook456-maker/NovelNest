package org.example.domain.worldsetting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

@Getter
public class WorldSettingCreateRequestDto {

    @Schema(description = "설정 분류 (COUNTRY / RACE / MAGIC / ORGANIZATION / PLACE / EVENT / ITEM / RULE / ETC)", example = "MAGIC")
    @NotNull
    private WorldSettingCategory category;

    @Schema(description = "설정 제목", example = "마법 사용 조건")
    @NotBlank
    private String title;

    @Schema(description = "설정 상세 내용", example = "마법은 핏줄이 있어야만 사용 가능하다. 일반인은 마법을 쓸 수 없다.")
    @NotBlank
    private String content;
}
