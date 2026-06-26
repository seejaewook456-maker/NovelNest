package org.example.domain.conflictdetection.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.conflictdetection.dto.ConflictDetectionResponseDto;
import org.example.domain.conflictdetection.dto.ConflictResultDto;
import org.example.domain.conflictdetection.entity.ConflictDetectionResult;
import org.example.domain.conflictdetection.repository.ConflictDetectionResultRepository;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.episodesummary.entity.EpisodeSummary;
import org.example.domain.episodesummary.repository.EpisodeSummaryRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.domain.worldsetting.entity.WorldSetting;
import org.example.domain.worldsetting.repository.WorldSettingRepository;
import org.example.global.ai.service.OpenAiService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ConflictDetectionService {

    // 작가의 창작을 존중하면서 충돌 가능성만 제안하도록 역할 지시
    private static final String DETECTION_INSTRUCTIONS =
            "당신은 소설 설정 충돌 탐지 전문가입니다. 주어진 회차 본문과 기존 설정(등장인물, 세계관, 이전 회차 요약)을 비교하여 충돌 가능성을 분석합니다.\n\n" +
            "반드시 다음 규칙을 따르세요:\n" +
            "1. 순수 JSON 배열만 반환하세요. 설명, 마크다운 코드블록 등 다른 텍스트는 절대 포함하지 마세요.\n" +
            "2. 충돌이 없으면 빈 배열 []만 반환하세요.\n" +
            "3. 작가의 창작을 검열하거나 수정하지 마세요. 충돌 가능성만 지적하세요.\n" +
            "4. 의도적인 반전이나 복선일 가능성을 항상 고려하세요. 확실하지 않으면 severity를 LOW로 설정하세요.\n" +
            "5. '검토 필요', '충돌 가능성이 있습니다' 같은 표현을 사용하고 단정하지 마세요.\n" +
            "6. type은 CHARACTER_CONFLICT / PERSONALITY_CONFLICT / RELATIONSHIP_CONFLICT / WORLD_SETTING_CONFLICT / ABILITY_CONFLICT / TIMELINE_CONFLICT 중 하나로 설정하세요.\n" +
            "7. severity 기준: HIGH(사망/생존 충돌, 나이 등 명확한 정보 충돌, 세계관 규칙 위반), MEDIUM(성격 급변, 관계 설정 애매, 능력 조건 불명확), LOW(추가 설명 권장, 독자 혼동 우려).\n" +
            "8. 동일한 충돌을 중복 보고하지 마세요.";

    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final WorldSettingRepository worldSettingRepository;
    private final EpisodeSummaryRepository episodeSummaryRepository;
    private final ConflictDetectionResultRepository conflictDetectionResultRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    // AI 충돌 탐지 실행 후 결과를 DB에 upsert
    @Transactional
    public ConflictDetectionResponseDto detectConflicts(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        Novel novel = episode.getNovel();
        validateOwner(novel, user);

        log.info("Conflict detection started. episodeId={}, userId={}", episodeId, user.getId());
        List<Character> characters = characterRepository.findAllByNovelOrderByIsFavoriteDescNameAsc(novel);
        List<WorldSetting> worldSettings = worldSettingRepository.findAllByNovelOrderByCategoryAscIsFavoriteDescTitleAsc(novel);

        List<EpisodeSummary> recentSummaries = episodeSummaryRepository
                .findRecentSummariesByNovel(novel, PageRequest.of(0, 10))
                .stream()
                .filter(s -> s.getEpisode().getEpisodeNumber() < episode.getEpisodeNumber())
                .collect(Collectors.toList());

        String input = buildInput(episode, novel, characters, worldSettings, recentSummaries);
        String aiResponse = openAiService.generateText(DETECTION_INSTRUCTIONS, input);
        List<ConflictResultDto> conflicts = parseJson(aiResponse);

        // 결과를 JSON으로 직렬화해 DB에 upsert — 재분석 시 기존 결과를 덮어씀
        String conflictsJson = serializeConflicts(conflicts);
        ConflictDetectionResult saved = conflictDetectionResultRepository.findByEpisode(episode)
                .map(existing -> {
                    existing.update(conflictsJson, conflicts.size());
                    return existing;
                })
                .orElseGet(() -> conflictDetectionResultRepository.save(
                        ConflictDetectionResult.builder()
                                .episode(episode)
                                .conflictsJson(conflictsJson)
                                .conflictCount(conflicts.size())
                                .build()
                ));

        log.info("Conflict detection complete. episodeId={}, conflictCount={}, userId={}", episodeId, conflicts.size(), user.getId());
        String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
        return new ConflictDetectionResponseDto(episodeTitle, conflicts, saved.getUpdatedAt());
    }

    // DB에서 저장된 충돌 탐지 결과 조회 — 없으면 null 반환
    @Transactional(readOnly = true)
    public ConflictDetectionResponseDto getConflictResult(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        validateOwner(episode.getNovel(), user);

        return conflictDetectionResultRepository.findByEpisode(episode)
                .map(result -> {
                    List<ConflictResultDto> conflicts = parseJson(result.getConflictsJson());
                    String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
                    return new ConflictDetectionResponseDto(episodeTitle, conflicts, result.getUpdatedAt());
                })
                .orElse(null);
    }

    // AI에게 전달하는 입력 텍스트 조립
    private String buildInput(Episode episode, Novel novel,
                              List<Character> characters,
                              List<WorldSetting> worldSettings,
                              List<EpisodeSummary> recentSummaries) {
        StringBuilder sb = new StringBuilder();

        // 작품 정보
        sb.append("[작품 정보]\n");
        sb.append("제목: ").append(novel.getTitle()).append("\n");
        sb.append("장르: ").append(novel.getGenre() != null ? novel.getGenre() : "없음").append("\n");
        sb.append("설명: ").append(novel.getDescription() != null ? novel.getDescription() : "없음").append("\n\n");

        // 현재 회차 본문
        sb.append("[현재 회차]\n");
        sb.append("회차 번호: ").append(episode.getEpisodeNumber()).append("화\n");
        sb.append("제목: ").append(episode.getTitle()).append("\n");
        sb.append("본문:\n").append(episode.getContent()).append("\n\n");

        // 등장인물 목록
        sb.append("[등장인물 목록]\n");
        if (characters.isEmpty()) {
            sb.append("등록된 등장인물이 없습니다.\n");
        } else {
            try {
                List<Map<String, Object>> charList = characters.stream()
                        .map(c -> {
                            Map<String, Object> map = new LinkedHashMap<>();
                            map.put("id", c.getId());
                            map.put("name", c.getName());
                            map.put("role", c.getRole() != null ? c.getRole() : "");
                            map.put("age", c.getAge() != null ? c.getAge() : "");
                            map.put("personality", c.getPersonality() != null ? c.getPersonality() : "");
                            map.put("speechStyle", c.getSpeechStyle() != null ? c.getSpeechStyle() : "");
                            map.put("description", c.getDescription() != null ? c.getDescription() : "");
                            return map;
                        })
                        .collect(Collectors.toList());
                sb.append(objectMapper.writeValueAsString(charList)).append("\n");
            } catch (Exception e) {
                sb.append("[]\n");
            }
        }
        sb.append("\n");

        // 세계관 설정 목록
        sb.append("[세계관 설정 목록]\n");
        if (worldSettings.isEmpty()) {
            sb.append("등록된 세계관 설정이 없습니다.\n");
        } else {
            try {
                List<Map<String, Object>> settingList = worldSettings.stream()
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
        sb.append("\n");

        // 이전 회차 요약 (회차 번호 오름차순 — 시간 순으로 맥락 파악)
        sb.append("[이전 회차 요약 (최근 최대 10개)]\n");
        if (recentSummaries.isEmpty()) {
            sb.append("이전 회차 요약이 없습니다.\n");
        } else {
            List<EpisodeSummary> ordered = recentSummaries.stream()
                    .sorted((a, b) -> Integer.compare(
                            a.getEpisode().getEpisodeNumber(),
                            b.getEpisode().getEpisodeNumber()))
                    .collect(Collectors.toList());
            for (EpisodeSummary es : ordered) {
                sb.append(es.getEpisode().getEpisodeNumber()).append("화 - ")
                  .append(es.getEpisode().getTitle()).append(": ")
                  .append(es.getSummary()).append("\n");
            }
        }
        sb.append("\n");

        // 출력 형식 지시
        sb.append("위 정보를 바탕으로 현재 회차에서 발견되는 충돌 가능성을 아래 형식의 JSON 배열로만 반환하세요:\n");
        sb.append("[{");
        sb.append("\"type\":\"CONFLICT_TYPE\",");
        sb.append("\"severity\":\"HIGH 또는 MEDIUM 또는 LOW\",");
        sb.append("\"title\":\"충돌 제목\",");
        sb.append("\"existingInfo\":\"기존 설정 내용\",");
        sb.append("\"currentEpisodeInfo\":\"현재 회차에서 발견된 내용\",");
        sb.append("\"description\":\"충돌 설명\",");
        sb.append("\"suggestion\":\"작가에게 전달하는 검토 제안\"");
        sb.append("}]");

        return sb.toString();
    }

    // OpenAI 응답 텍스트 → List<ConflictResultDto> 파싱
    private List<ConflictResultDto> parseJson(String jsonText) {
        if (jsonText == null || jsonText.isBlank()) {
            throw new IllegalArgumentException("AI 응답이 비어 있습니다. 다시 시도해 주세요.");
        }

        // AI가 마크다운 코드블록(```json ... ```)을 포함하는 경우 제거
        String cleaned = jsonText.replaceAll("(?s)```json\\s*|```\\s*", "").trim();

        try {
            return objectMapper.readValue(cleaned, new TypeReference<List<ConflictResultDto>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 응답 파싱에 실패했습니다. 다시 시도해 주세요. 원인: " + e.getMessage());
        }
    }

    // 충돌 목록을 JSON 문자열로 직렬화 — DB 저장용
    private String serializeConflicts(List<ConflictResultDto> conflicts) {
        try {
            return objectMapper.writeValueAsString(conflicts);
        } catch (Exception e) {
            throw new IllegalArgumentException("충돌 결과 직렬화에 실패했습니다: " + e.getMessage());
        }
    }

    private User findUserByEmail(String email) {
        return userRepository.findByEmail(email)
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
