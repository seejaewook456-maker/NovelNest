package org.example.domain.worldsettingextraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.airatelimit.service.AiRateLimitService;
import org.example.domain.aiusage.enums.AiFeatureType;
import org.example.domain.aiusage.service.AiUsageService;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.dto.WorldSettingResponseDto;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.domain.worldsettingextraction.dto.WorldSettingCandidateDto;
import org.example.domain.worldsettingextraction.dto.WorldSettingExtractionResponseDto;
import org.example.global.ai.service.OpenAiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorldSettingExtractionService {

    // 병합 시 기존 내용과 신규 내용 사이를 구분하는 빈 줄 — 반복 보강 시에도 정확히 이 구분자
    // 하나만 남도록 mergeWithExistingContent()가 기존 내용 끝의 개행을 먼저 정리한다.
    private static final String CONTENT_MERGE_SEPARATOR = "\n\n";

    private static final String EXTRACTION_INSTRUCTIONS =
            "당신은 소설 세계관 분석 전문가입니다. 주어진 소설 회차를 분석하여 세계관/설정 후보를 추출합니다.\n\n" +
            "반드시 다음 규칙을 따르세요:\n" +
            "1. 순수 JSON 배열만 반환하세요. 설명, 마크다운 코드블록 등 다른 텍스트는 절대 포함하지 마세요.\n" +
            "2. 기존 세계관 설정 목록이 제공된 경우, 본문에 등장하는 기존 설정을 반드시 식별하세요.\n" +
            "3. 기존 설정은 isExistingSetting을 true로, matchedWorldSettingId에 해당 ID를 설정하세요.\n" +
            "4. 신규 설정은 isExistingSetting을 false로, matchedWorldSettingId를 null로 설정하세요.\n" +
            "5. newInsights는 기존 설정에만 사용하며, 기존 content에 없는 새로운 정보만 짧은 문장 리스트로 포함하세요.\n" +
            "6. evidence는 해당 설정을 뒷받침하는 회차 내 구체적인 장면이나 대사를 기록하세요.\n" +
            "7. category는 반드시 COUNTRY/RACE/MAGIC/ORGANIZATION/PLACE/EVENT/ITEM/RULE/ETC 중 하나로 설정하세요.\n" +
            "8. 모든 문자열 값은 유효한 JSON 문자열이어야 합니다. 값 안에 큰따옴표(\")가 필요하면 반드시 \\\"로 이스케이프하거나 " +
            "작은따옴표(')로 바꿔 쓰세요. 줄바꿈이 필요하면 \\n으로 이스케이프하세요. 대사를 인용할 때 특히 주의하세요.\n" +
            "9. content(신규 설정) 또는 newContent(기존 설정 보강)를 작성할 때 모든 문장을 하나의 문단으로 이어쓰지 마세요. " +
            "같은 맥락의 문장은 한 문단으로 유지하되, 주제가 전환되거나 서로 다른 규칙·개념으로 넘어갈 때는 문단 사이에 " +
            "빈 줄(\\n\\n)을 넣어 구분하세요. 단, 문장 단위로 무조건 줄바꿈하거나 과도하게 짧은 문단을 여러 개 만들지 말고, " +
            "읽기 자연스러운 문단 구조를 우선하세요.\n" +
            "10. 기존 설정을 보강하는 경우 newContent에는 기존 content를 반복하거나 요약하지 말고, 이번 회차에서 새롭게 " +
            "확인된 내용만 9번 규칙에 따른 문단으로 작성하세요. content에는 참고용으로 기존 내용과 newContent를 이어붙인 " +
            "전체 내용을 작성하되, 기존 내용 자체를 임의로 수정·요약하지 마세요(실제 저장 시 서버가 기존 내용을 기준으로 " +
            "다시 병합합니다).";

    private final EpisodeRepository episodeRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;
    private final AiUsageService aiUsageService;
    private final AiRateLimitService aiRateLimitService;

    @Transactional(readOnly = true)
    public WorldSettingExtractionResponseDto extractWorldSettings(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        Novel novel = episode.getNovel();
        validateOwner(novel, user);

        // 기존 세계관 설정 목록 — AI가 신규/기존 설정을 구분하는 데 사용
        List<WorldSetting> existingSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);

        // OpenAI 호출 — DB 저장 없음
        String input = buildInput(episode, novel, existingSettings);
        // 분당 Rate Limit → 하루 사용량 제한 순으로 검사한 뒤에만 OpenAI를 호출한다.
        aiRateLimitService.checkAndRecord(user.getId());
        aiUsageService.checkAndIncrement(user.getId(), AiFeatureType.WORLDVIEW_EXTRACTION);
        String aiResponse = openAiService.generateText(EXTRACTION_INSTRUCTIONS, input);
        List<WorldSettingCandidateDto> candidates = parseJson(aiResponse);

        // matchedWorldSettingId로 기존 WorldSetting을 조회해 existingWorldSetting 필드 보강
        Map<Long, WorldSetting> settingMap = existingSettings.stream()
                .collect(Collectors.toMap(WorldSetting::getId, s -> s));

        for (WorldSettingCandidateDto candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.getIsExistingSetting())
                    && candidate.getMatchedWorldSettingId() != null) {
                WorldSetting matched = settingMap.get(candidate.getMatchedWorldSettingId());
                if (matched != null) {
                    candidate.setExistingWorldSetting(WorldSettingResponseDto.from(matched));
                    // AI가 content에 기존 내용을 그대로 옮기며 재작성/요약할 위험이 있으므로,
                    // newContent(신규 보강분)만 신뢰하고 기존 내용은 DB 값을 그대로 사용해
                    // 서버가 직접 "기존 내용 + 빈 줄 + 신규 내용" 구조로 다시 병합한다.
                    if (candidate.getNewContent() != null && !candidate.getNewContent().isBlank()) {
                        candidate.setContent(mergeWithExistingContent(matched.getContent(), candidate.getNewContent()));
                    }
                }
            }
        }

        log.info("WorldSetting extraction complete. episodeId={}, candidateCount={}, userId={}", episodeId, candidates.size(), user.getId());
        String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
        return new WorldSettingExtractionResponseDto(episodeTitle, candidates.size(), candidates);
    }

    // AI에게 전달하는 입력 텍스트 조립
    private String buildInput(Episode episode, Novel novel, List<WorldSetting> existingSettings) {
        StringBuilder sb = new StringBuilder();

        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("장르: ").append(novel.getGenre() != null ? novel.getGenre() : "없음").append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        sb.append("[회차 정보]\n");
        sb.append("회차 번호: ").append(episode.getEpisodeNumber()).append("화\n");
        sb.append("제목: ").append(episode.getTitle()).append("\n");
        sb.append("본문:\n").append(episode.getContent()).append("\n\n");

        sb.append("[기존 세계관 설정 목록]\n");
        if (existingSettings.isEmpty()) {
            sb.append("등록된 세계관 설정이 없습니다.\n");
        } else {
            try {
                List<Map<String, Object>> settingList = existingSettings.stream()
                        .map(s -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("id", s.getId());
                            map.put("category", s.getCategory().name());
                            map.put("title", s.getTitle());
                            map.put("content", s.getContent());
                            return map;
                        })
                        .collect(Collectors.toList());
                sb.append(objectMapper.writeValueAsString(settingList)).append("\n");
            } catch (Exception e) {
                sb.append("[]\n");
            }
        }

        sb.append("\n위 회차에서 세계관/설정 후보를 분석하여 아래 형식의 JSON 배열로만 반환하세요:\n");
        sb.append("[{");
        sb.append("\"category\":\"COUNTRY 또는 RACE 또는 MAGIC 또는 ORGANIZATION 또는 PLACE 또는 EVENT 또는 ITEM 또는 RULE 또는 ETC\",");
        sb.append("\"title\":\"설정 제목\",");
        sb.append("\"content\":\"설정 내용 (신규 설정은 전체 내용, 기존 설정 보강은 참고용으로 기존 내용+newContent를 합친 전체 내용)\",");
        sb.append("\"evidence\":\"회차 내 근거 장면 또는 대사\",");
        sb.append("\"isExistingSetting\":true 또는 false,");
        sb.append("\"matchedWorldSettingId\":기존설정ID 또는 null,");
        sb.append("\"newInsights\":{\"content\":[\"새로운 정보1\",\"새로운 정보2\"]} 또는 null,");
        sb.append("\"newContent\":\"기존 설정 보강 시 이번 회차에서 새로 추가되는 문단 내용(문맥 단위 줄바꿈 적용)\" 또는 null");
        sb.append("}]");

        return sb.toString();
    }

    // 기존 세계관 내용 뒤에 신규 보강 내용을 정확히 빈 줄 하나로 구분해 붙인다.
    // 기존 내용 끝의 개행/공백을 먼저 제거한 뒤 구분자를 붙이므로, 여러 회차에 걸쳐 반복
    // 보강해도 빈 줄이 2개 이상으로 누적되지 않는다.
    private String mergeWithExistingContent(String existingContent, String newContent) {
        String trimmedExisting = existingContent == null ? "" : stripTrailingBlank(existingContent);
        String trimmedNew = newContent.strip();

        if (trimmedExisting.isEmpty()) {
            return trimmedNew;
        }
        return trimmedExisting + CONTENT_MERGE_SEPARATOR + trimmedNew;
    }

    // 문자열 끝의 개행/공백 문자만 제거 (앞부분과 중간 내용은 그대로 보존)
    private String stripTrailingBlank(String text) {
        int end = text.length();
        while (end > 0 && Character.isWhitespace(text.charAt(end - 1))) {
            end--;
        }
        return text.substring(0, end);
    }

    // OpenAI 응답 텍스트 → List<WorldSettingCandidateDto> 파싱
    private List<WorldSettingCandidateDto> parseJson(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다. 다시 시도해 주세요.");
        }

        // AI가 마크다운 코드블록(```json ... ```)을 포함하는 경우 제거
        String cleaned = jsonText.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

        try {
            return objectMapper.readValue(cleaned, new TypeReference<List<WorldSettingCandidateDto>>() {});
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
