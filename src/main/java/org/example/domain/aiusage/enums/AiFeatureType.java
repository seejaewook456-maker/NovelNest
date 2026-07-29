package org.example.domain.aiusage.enums;

// AI 기능별 하루 사용량 제한을 적용하는 기능 목록. 문자열을 여러 곳에 직접 적지 않고
// 이 enum과 label(사용자에게 보여줄 한글 이름)로 중앙 관리한다.
public enum AiFeatureType {

    EPISODE_SUMMARY("회차 요약"),
    CONFLICT_DETECTION("설정 충돌 감지"),
    CHARACTER_EXTRACTION("등장인물 추출"),
    WORLDVIEW_EXTRACTION("세계관 추출"),
    AI_CHAT("AI 챗봇");

    private final String label;

    AiFeatureType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
