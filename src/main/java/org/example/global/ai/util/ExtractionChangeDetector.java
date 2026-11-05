package org.example.global.ai.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// AI 추출 결과(등장인물/세계관)에서 "기존 데이터에 이미 있는 내용"을 걸러내는 공통 유틸.
//
// 의미 비교는 1차로 AI 프롬프트가 담당하고, 이 클래스는 AI가 규칙을 어기고
// 기존 내용을 그대로 반복해 돌려준 경우를 막는 2차 안전장치다.
// 그래서 "확실히 같은 것"(공백·문장부호·대소문자만 다른 경우, 기존 문장에 그대로 포함된 경우)만
// 제거하고, 애매한 경우는 남겨서 실제 새 정보가 누락되지 않도록 한다.
public final class ExtractionChangeDetector {

    private ExtractionChangeDetector() {
    }

    // 비교용 정규화 — 글자(한글/영문 등)와 숫자만 남기고 소문자로 통일한다.
    // 예) "냉정함, 침착함." → "냉정함침착함"
    public static String normalize(String text) {
        if (text == null) {
            return "";
        }
        return text.replaceAll("[^\\p{L}\\p{N}]", "").toLowerCase();
    }

    // 후보 문장이 기존 텍스트들 중 하나에 이미 포함되어 있는지 확인한다.
    // 정규화 결과가 빈 문자열(공백·기호뿐인 값)도 "새 정보 아님"으로 취급한다.
    public static boolean isAlreadyCovered(String candidate, String... existingTexts) {
        String normalizedCandidate = normalize(candidate);
        if (normalizedCandidate.isEmpty()) {
            return true;
        }
        for (String existing : existingTexts) {
            if (normalize(existing).contains(normalizedCandidate)) {
                return true;
            }
        }
        return false;
    }

    // 새 정보 목록에서 빈 값·중복·기존 텍스트에 이미 포함된 항목을 제거한 목록을 반환한다.
    // 입력이 null이면 빈 목록을 반환한다 (null 체크를 호출부마다 반복하지 않기 위함).
    public static List<String> filterNewItems(List<String> items, String... existingTexts) {
        List<String> result = new ArrayList<>();
        if (items == null) {
            return result;
        }
        Set<String> seen = new LinkedHashSet<>();
        for (String item : items) {
            if (item == null || isAlreadyCovered(item, existingTexts)) {
                continue;
            }
            // 같은 응답 안에서 표현만 다른 중복 항목도 하나만 남긴다
            if (seen.add(normalize(item))) {
                result.add(item.strip());
            }
        }
        return result;
    }

    // 단일 값(역할 등)이 기존 값과 실질적으로 다른지 확인한다.
    // 빈 값이거나 정규화 후 동일하면 "변경 없음"으로 본다.
    public static boolean isChangedValue(String candidate, String existing) {
        String normalizedCandidate = normalize(candidate);
        return !normalizedCandidate.isEmpty() && !normalizedCandidate.equals(normalize(existing));
    }

    // 숫자 값(나이 등)이 기존 값과 다른지 확인한다. 후보가 null이면 "변경 없음".
    public static boolean isChangedValue(Integer candidate, Integer existing) {
        return candidate != null && !Objects.equals(candidate, existing);
    }

    // ===== 세계관 설명 통합 검증용 =====

    // 회차·분석 과정을 서술하는 메타 표현 — 세계관 설명 자체에는 들어가면 안 되는 문구들.
    // 세계관 안의 사건 서술("진실이 밝혀졌다" 등)까지 막지 않도록 "회차"/"새롭게 확인" 같은 분석 어휘 위주로 잡는다.
    private static final Pattern EPISODE_NARRATION = Pattern.compile(
            "(이번|이전|지난|앞선|다음)\\s*회차"
                    + "|이번에\\s*새롭게"
                    + "|새롭게\\s*(밝혀진|확인된|드러난|추가된)"
                    + "|추가된\\s*설정"
                    + "|기존\\s*설정에\\s*따르면"
                    + "|이번\\s*내용을\\s*통해"
                    + "|(는|라는|다는)\\s*(사실|점)이\\s*(드러났|확인되었|확인됐|밝혀졌)");

    // 문장 맨 앞에 붙은 회차 언급만 안전하게 잘라낼 수 있는 패턴 (나머지 문장은 그대로 사실 서술이 됨)
    // 예) "이번 회차에서는 북쪽 골목에 ..." → "북쪽 골목에 ..."
    private static final Pattern LEADING_EPISODE_PHRASE = Pattern.compile(
            "(?:^|(?<=[.!?。]\\s{1,3})|(?<=\\n))(이번|이전|지난)\\s*회차(에서는|에서|를\\s*통해|와\\s*달리)\\s*,?\\s*");

    // 회차 중심 표현이 포함되어 있는지 확인한다
    public static boolean containsEpisodeNarration(String text) {
        return text != null && EPISODE_NARRATION.matcher(text).find();
    }

    // 문장 맨 앞의 "이번 회차에서는" 류 표현만 제거한다. 그 외 표현은 의미가 바뀔 수 있어 건드리지 않는다.
    public static String stripEpisodeNarration(String text) {
        if (text == null) {
            return null;
        }
        return LEADING_EPISODE_PHRASE.matcher(text).replaceAll("");
    }

    // 문단 구분 정리 — 개행 통일, 줄 끝 공백 제거, 3줄 이상 연속 개행은 빈 줄 하나(\n\n)로 축소
    public static String normalizeParagraphs(String text) {
        if (text == null) {
            return "";
        }
        return text.replace("\r\n", "\n")
                .replaceAll("[ \\t]+\\n", "\n")
                .replaceAll("\\n{3,}", "\n\n")
                .strip();
    }

    // 기존 설명에서 "반드시 보존해야 하는 핵심 용어" — 숫자와 따옴표/낫표로 감싼 고유명사.
    // 한국어는 형태소 분석 없이 일반 명사를 정확히 뽑기 어려우므로, 오탐 없이 확실히 식별되는 것만 검사한다.
    private static final Pattern KEY_TERM = Pattern.compile(
            "\\d+(?:[.,]\\d+)*"
                    + "|'([^'\\n]{1,40})'"
                    + "|\"([^\"\\n]{1,40})\""
                    + "|‘([^’\\n]{1,40})’"
                    + "|“([^”\\n]{1,40})”"
                    + "|「([^」\\n]{1,40})」"
                    + "|『([^』\\n]{1,40})』");

    // target(재구성 설명)이 source(기존 설명)의 핵심 용어를 모두 포함하는지 확인한다.
    // 따옴표 자체는 없어져도 되고(정규화 후 비교), 안의 이름만 남아 있으면 통과한다.
    public static boolean keepsKeyTerms(String source, String target) {
        if (source == null || source.isBlank()) {
            return true;
        }
        String normalizedTarget = normalize(target);
        Matcher m = KEY_TERM.matcher(source);
        while (m.find()) {
            String term = m.group();
            for (int g = 1; g <= m.groupCount(); g++) {
                if (m.group(g) != null) {
                    term = m.group(g);
                    break;
                }
            }
            String normalizedTerm = normalize(term);
            if (!normalizedTerm.isEmpty() && !normalizedTarget.contains(normalizedTerm)) {
                return false;
            }
        }
        return true;
    }

    // source(기존 설명)의 내용이 target(통합 설명)에 얼마나 남아 있는지 0.0~1.0으로 계산한다.
    // 정규화한 문자열의 "연속된 두 글자(bigram)" 중 target에도 존재하는 비율을 쓴다.
    // 단어 순서나 조사가 조금 바뀌어도 대부분의 bigram은 유지되므로, 자연스러운 다듬기는 허용하고
    // 기존 문장을 통째로 빼거나 요약한 경우만 낮은 값이 나온다.
    public static double coverageRatio(String source, String target) {
        String s = normalize(source);
        String t = normalize(target);
        if (s.length() < 2) {
            return t.contains(s) ? 1.0 : 0.0;
        }
        Set<String> targetBigrams = new HashSet<>();
        for (int i = 0; i + 2 <= t.length(); i++) {
            targetBigrams.add(t.substring(i, i + 2));
        }
        int total = 0;
        int covered = 0;
        for (int i = 0; i + 2 <= s.length(); i++) {
            total++;
            if (targetBigrams.contains(s.substring(i, i + 2))) {
                covered++;
            }
        }
        return (double) covered / total;
    }
}
