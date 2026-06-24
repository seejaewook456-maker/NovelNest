package org.example.domain.character.dto;

import lombok.Getter;
import org.example.domain.character.entity.Character;

import java.time.LocalDateTime;

@Getter
public class CharacterResponseDto {

    private final Long id;
    private final Long novelId;
    private final String name;
    private final String role;
    private final Integer age;
    private final String personality;
    private final String speechStyle;
    private final String description;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private CharacterResponseDto(Long id, Long novelId, String name, String role, Integer age,
                                  String personality, String speechStyle, String description,
                                  LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.novelId = novelId;
        this.name = name;
        this.role = role;
        this.age = age;
        this.personality = personality;
        this.speechStyle = speechStyle;
        this.description = description;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static CharacterResponseDto from(Character character) {
        return new CharacterResponseDto(
                character.getId(),
                character.getNovel().getId(),
                character.getName(),
                character.getRole(),
                character.getAge(),
                character.getPersonality(),
                character.getSpeechStyle(),
                character.getDescription(),
                character.getCreatedAt(),
                character.getUpdatedAt()
        );
    }
}
