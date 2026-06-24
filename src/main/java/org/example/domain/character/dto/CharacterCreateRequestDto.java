package org.example.domain.character.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;

@Getter
public class CharacterCreateRequestDto {

    @NotBlank
    private String name;

    private String role;

    @Min(0)
    private Integer age;

    private String personality;

    private String speechStyle;

    private String description;
}
