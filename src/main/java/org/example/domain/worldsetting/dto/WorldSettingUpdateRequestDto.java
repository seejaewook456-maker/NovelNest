package org.example.domain.worldsetting.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

@Getter
public class WorldSettingUpdateRequestDto {

    @Schema(description = "설정 분류 (COUNTRY / RACE / MAGIC / ORGANIZATION / PLACE / EVENT / ITEM / RULE / ETC)", example = "RULE")
    @NotNull
    private WorldSettingCategory category;

    @Schema(description = "설정 제목", example = "마법 사용 규칙 (개정)")
    @NotBlank
    private String title;

    @Schema(description = "설정 상세 내용", example = "핏줄 있는 자만 사용 가능. 단, 각성 의식 필요.")
    @NotBlank
    private String content;
}
