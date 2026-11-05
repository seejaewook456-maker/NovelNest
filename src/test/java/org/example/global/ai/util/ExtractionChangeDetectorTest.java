package org.example.global.ai.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExtractionChangeDetectorTest {

    @Test
    void 회차_중심_표현을_감지한다() {
        assertThat(ExtractionChangeDetector.containsEpisodeNarration("이번 회차에서는 관리소가 등장했다.")).isTrue();
        assertThat(ExtractionChangeDetector.containsEpisodeNarration("새롭게 확인된 사실은 신원이 보호된다는 것이다.")).isTrue();
        assertThat(ExtractionChangeDetector.containsEpisodeNarration("거래소가 존재한다는 사실이 드러났다.")).isTrue();
        assertThat(ExtractionChangeDetector.containsEpisodeNarration("이전 회차와 달리 구조가 설명되었다.")).isTrue();
        // 세계관 내부 사실 서술은 감지하지 않는다
        assertThat(ExtractionChangeDetector.containsEpisodeNarration(
                "중앙 거리 끝에는 기록 관리소가 위치해 있다. 왕의 죽음 이후 진실이 밝혀졌다.")).isFalse();
    }

    @Test
    void 문장앞_회차_표현만_제거한다() {
        assertThat(ExtractionChangeDetector.stripEpisodeNarration("이번 회차에서는 A가 있다. 이번 회차에서, B가 있다.\n이전 회차와 달리 C다."))
                .isEqualTo("A가 있다. B가 있다.\nC다.");
    }

    @Test
    void 핵심_용어_보존_여부를_검사한다() {
        String existing = "'크로아 관리소'는 기록을 30년간 보관하며 「봉인 규약」을 따른다.";
        assertThat(ExtractionChangeDetector.keepsKeyTerms(existing,
                "크로아 관리소는 봉인 규약에 따라 기록을 30년 동안 보관한다.")).isTrue();
        assertThat(ExtractionChangeDetector.keepsKeyTerms(existing,
                "크로아 관리소는 봉인 규약에 따라 기록을 오래 보관한다.")).isFalse();
        assertThat(ExtractionChangeDetector.keepsKeyTerms(existing,
                "관리소는 봉인 규약에 따라 기록을 30년 동안 보관한다.")).isFalse();
    }

    @Test
    void 기존_설명_보존률을_계산한다() {
        String existing = "기억 시장에는 공식 거래소뿐 아니라 개인 간 거래와 비공식 거래도 존재한다.";
        String integrated = existing + " 특히 북쪽 골목의 낡은 지역에는 비공식 기억 거래 장소들이 형성되어 있으며, 전문 중개인들이 거래를 연결한다.";
        assertThat(ExtractionChangeDetector.coverageRatio(existing, integrated)).isEqualTo(1.0);
        assertThat(ExtractionChangeDetector.coverageRatio(existing, "북쪽 골목에 거래 장소가 있다.")).isLessThan(0.8);
    }
}
