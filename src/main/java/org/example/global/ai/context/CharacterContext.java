package org.example.global.ai.context;

// AI 프롬프트에 전달할 등장인물 정보 — Character 엔티티를 직접 노출하지 않기 위한 DTO.
// 엔티티에 실제로 존재하는 필드만 담는다.
public record CharacterContext(
        String name,
        String role,
        Integer age,
        String personality,
        String speechStyle,
        String description
) {
}
