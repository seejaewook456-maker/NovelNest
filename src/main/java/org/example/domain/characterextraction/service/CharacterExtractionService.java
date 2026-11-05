package org.example.domain.characterextraction.service;

import com.fasterxml.jackson.core.json.JsonReadFeature;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.airatelimit.service.AiRateLimitService;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.characterextraction.dto.CharacterCandidateDto;
import org.example.domain.characterextraction.dto.CharacterExtractionResponseDto;
import org.example.domain.characterextraction.dto.NewInsightsDto;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.ai.service.OpenAiService;
import org.example.global.ai.util.ExtractionChangeDetector;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CharacterExtractionService {

    // AI 역할 지시문 — 구조화된 JSON 반환을 강제
    private static final String EXTRACTION_INSTRUCTIONS =
            "당신은 소설 분석 전문가입니다. 주어진 소설 회차를 분석하여 등장인물 후보를 추출합니다.\n\n" +
            "반드시 다음 규칙을 따르세요:\n" +
            "1. 순수 JSON 배열만 반환하세요. 설명, 마크다운 코드블록 등 다른 텍스트는 절대 포함하지 마세요.\n" +
            "2. 기존 등장인물 목록이 제공된 경우, 회차에 등장하는 기존 인물을 반드시 식별하세요.\n" +
            "3. 기존 인물은 isExistingCharacter를 true로, matchedCharacterId에 해당 ID를 설정하세요.\n" +
            "4. 신규 인물은 isExistingCharacter를 false로, matchedCharacterId를 null로 설정하세요.\n" +
            "5. newInsights는 기존 인물에만 사용하며, 기존 정보에 없는 새로운 성격·말투만 리스트로 포함하세요. " +
            "외모·능력·배경·관계 등 설명(description)에 보완할 새 정보는 newInsights.description 리스트에 짧은 문장으로 포함하세요. " +
            "역할(role)이나 나이(age)가 기존 정보와 다르게 확인된 경우에만 newInsights.role / newInsights.age에 새 값을 설정하고, " +
            "변경이 없으면 null로 두세요.\n" +
            "6. evidence는 해당 특성을 뒷받침하는 회차 내 구체적인 장면이나 대사를 기록하세요.\n" +
            "7. age는 반드시 숫자(정수) 또는 null로 설정하세요. 문자열로 작성하지 마세요.\n" +
            "8. 모든 문자열 값은 유효한 JSON 문자열이어야 합니다. 값 안에 큰따옴표(\")가 필요하면 반드시 \\\"로 이스케이프하거나 " +
            "작은따옴표(')로 바꿔 쓰세요. 작은따옴표 앞에는 백슬래시(\\)를 붙이지 마세요(\\'는 잘못된 JSON입니다). " +
            "줄바꿈이 필요하면 \\n으로 이스케이프하세요. 대사를 인용할 때 특히 주의하세요.\n" +
            "9. 기존 인물은 기존 정보(role, age, personality, speechStyle, description)와 이번 회차 내용을 의미적으로 비교하세요. " +
            "실제로 추가하거나 수정할 새로운 정보가 하나도 없다면 그 인물은 결과 배열에 절대 포함하지 마세요. " +
            "(단순히 다시 등장·언급된 경우, 기존 정보와 같은 내용, 표현만 다르고 의미가 같은 내용, 기존 정보에 이미 포함된 내용, " +
            "단순한 문장 재구성이나 어휘 변경은 모두 '새로운 정보 없음'입니다.)\n" +
            "10. 본문에서 근거를 찾을 수 없거나 변경 여부가 불확실한 정보는 newInsights에 넣지 마세요.\n" +
            "11. 결과에 포함할 인물이 하나도 없으면 빈 배열 []을 반환하세요.";

    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final AiRateLimitService aiRateLimitService;

    @Transactional(readOnly = true)
    public CharacterExtractionResponseDto extractCharacters(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        Novel novel = episode.getNovel();
        validateOwner(novel, user);

        // 기존 등장인물 목록 — AI가 신규/기존 인물을 구분하는 데 사용
        List<Character> existingCharacters = characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel);

        // OpenAI 호출 — DB 저장 없음
        String input = buildInput(episode, novel, existingCharacters);
        // 분당 Rate Limit → 하루 사용량 제한 순으로 검사한 뒤에만 OpenAI를 호출한다.
        // (이 메서드는 readOnly 트랜잭션이지만 두 검사 모두 REQUIRES_NEW로 독립 커밋되므로 문제없다.)
        aiRateLimitService.checkAndRecord(user.getId());
        aiUsageService.checkAndIncrement(user.getId(), AiFeatureType.CHARACTER_EXTRACTION);
        String aiResponse = openAiService.generateText(EXTRACTION_INSTRUCTIONS, input);
        List<CharacterCandidateDto> candidates = parseJson(aiResponse);

        // matchedCharacterId로 기존 Character를 조회해 existingCharacter 필드 보강
        Map<Long, Character> characterMap = existingCharacters.stream()
                .collect(Collectors.toMap(Character::getId, c -> c));

        // 변경사항이 있는 후보만 담는다 — 기존 인물이 새 정보 없이 다시 등장한 경우는 제외
        List<CharacterCandidateDto> result = new ArrayList<>();
        for (CharacterCandidateDto candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.getIsExistingCharacter())
                    && candidate.getMatchedCharacterId() != null) {
                Character matched = characterMap.get(candidate.getMatchedCharacterId());
                if (matched != null) {
                    // AI가 규칙을 어기고 기존 내용을 반복했을 수 있으므로 서버에서 한 번 더 걸러낸다
                    if (!removeAlreadyKnownInsights(candidate, matched)) {
                        continue;
                    }
                    candidate.setExistingCharacter(CharacterResponseDto.from(matched));
                }
            }
            result.add(candidate);
        }

        log.info("Character extraction complete. episodeId={}, candidateCount={}, excludedCount={}, userId={}",
                episodeId, result.size(), candidates.size() - result.size(), user.getId());
        String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
        return new CharacterExtractionResponseDto(episodeTitle, result.size(), result);
    }

    // 기존 인물 후보의 newInsights에서 이미 DB에 있는 내용을 제거하고,
    // 실제로 추가·수정할 정보가 남아 있으면 true를 반환한다.
    // (DB 엔티티는 읽기만 하고 수정하지 않는다 — 응답 DTO만 정리)
    private boolean removeAlreadyKnownInsights(CharacterCandidateDto candidate, Character existing) {
        NewInsightsDto insights = candidate.getNewInsights();
        if (insights == null) {
            return false;
        }

        // 성격/말투/설명 보완 항목은 "기존 인물 정보 전체" 중 어디에든 이미 있으면 새 정보가 아니다
        String[] existingTexts = {
                existing.getRole(), existing.getPersonality(), existing.getSpeechStyle(), existing.getDescription()
        };
        insights.setPersonality(ExtractionChangeDetector.filterNewItems(insights.getPersonality(), existingTexts));
        insights.setSpeechStyle(ExtractionChangeDetector.filterNewItems(insights.getSpeechStyle(), existingTexts));
        insights.setDescription(ExtractionChangeDetector.filterNewItems(insights.getDescription(), existingTexts));

        // 역할/나이는 기존 값과 실제로 다를 때만 수정 제안으로 남긴다
        if (!ExtractionChangeDetector.isChangedValue(insights.getRole(), existing.getRole())) {
            insights.setRole(null);
        }
        if (!ExtractionChangeDetector.isChangedValue(insights.getAge(), existing.getAge())) {
            insights.setAge(null);
        }

        return !insights.getPersonality().isEmpty()
                || !insights.getSpeechStyle().isEmpty()
                || !insights.getDescription().isEmpty()
                || insights.getRole() != null
                || insights.getAge() != null;
    }

    // AI에게 전달하는 입력 텍스트 빌드
    private String buildInput(Episode episode, Novel novel, List<Character> existingCharacters) {
        StringBuilder sb = new StringBuilder();

        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        sb.append("[회차 정보]\n");
        sb.append("회차 번호: ").append(episode.getEpisodeNumber()).append("화\n");
        sb.append("제목: ").append(episode.getTitle()).append("\n");
        sb.append("본문:\n").append(episode.getContent()).append("\n\n");

        sb.append("[기존 등장인물 목록]\n");
        if (existingCharacters.isEmpty()) {
            sb.append("등록된 등장인물이 없습니다.\n");
        } else {
            // ObjectMapper로 특수문자를 안전하게 이스케이프 처리
            try {
                List<Map<String, Object>> charList = existingCharacters.stream()
                        .map(c -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("id", c.getId());
                            map.put("name", c.getName());
                            map.put("role", c.getRole() != null ? c.getRole() : "");
                            map.put("age", c.getAge());
                            map.put("personality", c.getPersonality() != null ? c.getPersonality() : "");
                            map.put("speechStyle", c.getSpeechStyle() != null ? c.getSpeechStyle() : "");
                            // 설명까지 전달해야 AI가 "이미 알고 있는 정보"인지 의미적으로 비교할 수 있다
                            map.put("description", c.getDescription() != null ? c.getDescription() : "");
                            return map;
                        })
                        .collect(Collectors.toList());
                sb.append(objectMapper.writeValueAsString(charList)).append("\n");
            } catch (Exception e) {
                sb.append("[]\n");
            }
        }

        sb.append("\n위 회차에서 등장인물을 분석하여 아래 형식의 JSON 배열로만 반환하세요:\n");
        sb.append("[{");
        sb.append("\"name\":\"인물명\",");
        sb.append("\"role\":\"역할 또는 null\",");
        sb.append("\"age\":나이숫자 또는 null,");
        sb.append("\"personality\":\"성격\",");
        sb.append("\"speechStyle\":\"말투\",");
        sb.append("\"description\":\"설명\",");
        sb.append("\"evidence\":\"회차 내 근거 장면 또는 대사\",");
        sb.append("\"isExistingCharacter\":true 또는 false,");
        sb.append("\"matchedCharacterId\":기존인물ID 또는 null,");
        sb.append("\"newInsights\":{\"personality\":[\"새 성격\"],\"speechStyle\":[\"새 말투\"],")
                .append("\"description\":[\"외모·능력·배경·관계 등 새로 보완할 정보\"],")
                .append("\"role\":\"기존과 달라진 역할 또는 null\",\"age\":기존과 달라진 나이숫자 또는 null} 또는 null");
        sb.append("}]\n");
        sb.append("기존 인물 중 새로 추가·수정할 정보가 없는 인물은 배열에서 제외하고, 포함할 인물이 없으면 []만 반환하세요.");

        return sb.toString();
    }

    // OpenAI 응답 텍스트 → List<CharacterCandidateDto> 파싱
    private List<CharacterCandidateDto> parseJson(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다. 다시 시도해 주세요.");
        }

        // AI가 마크다운 코드블록(```json ... ```)을 포함하는 경우 제거
        String cleaned = jsonText.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

        try {
            // AI가 대사를 인용하며 JSON에서 허용되지 않는 이스케이프(\' 등)를 쓰는 경우가 있어,
            // 이 응답을 읽을 때만 "백슬래시 뒤 임의 문자"를 허용한다 (\' → '). 공용 ObjectMapper 설정은 바꾸지 않는다.
            return objectMapper.readerFor(new TypeReference<List<CharacterCandidateDto>>() {})
                    .with(JsonReadFeature.ALLOW_BACKSLASH_ESCAPING_ANY_CHARACTER)
                    .readValue(cleaned);
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 응답 파싱에 실패했습니다. 다시 시도해 주세요. 원인: " + e.getMessage());
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmailAndDeletedAtIsNull(email)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
    }

    private Episode findEpisodeById(Long episodeId) {
        return episodeRepository.findById(episodeId)
                .orElseThrow(() -> new IllegalArgumentException("회차를 찾을 수 없습니다."));
    }

    // episode → novel → user 체인으로 소유권 검증
    private void validateOwner(Novel novel, User user) {
        if (!novel.getUser().getId().equals(user.getId())) {
            throw new SecurityException("해당 회차에 대한 권한이 없습니다.");
        }
    }
}
