package org.example.domain.worldsetting.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import org.example.domain.worldsetting.entity.WorldSettingCategory;

@Getter
public class WorldSettingUpdateRequestDto {

    @NotNull
    private WorldSettingCategory category;

    @NotBlank
    private String title;

    @NotBlank
    private String content;
}
