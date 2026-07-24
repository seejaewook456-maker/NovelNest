package org.example.global.ai.context;

// AI 프롬프트에 전달할 세계관 정보 — WorldSetting 엔티티를 직접 노출하지 않기 위한 DTO.
public record WorldSettingContext(
        String category,
        String title,
        String content
) {
}
