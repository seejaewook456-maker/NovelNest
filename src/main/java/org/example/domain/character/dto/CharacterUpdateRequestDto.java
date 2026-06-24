package org.example.domain.character.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CharacterUpdateRequestDto {

    @Schema(description = "등장인물 이름", example = "이한")
    @NotBlank
    private String name;

    @Schema(description = "역할 (주인공 / 조연 / 악역 등, 선택)", example = "주인공")
    private String role;

    @Schema(description = "나이 (미정인 경우 생략 가능)", example = "25")
    @Min(0)
    private Integer age;

    @Schema(description = "성격 (선택)", example = "성장 후 더 유연해짐.")
    private String personality;

    @Schema(description = "말투 (선택)", example = "조금 더 부드러워짐.")
    private String speechStyle;

    @Schema(description = "기타 설명 (선택)", example = "기사단 복귀 후 변화.")
    private String description;
}
