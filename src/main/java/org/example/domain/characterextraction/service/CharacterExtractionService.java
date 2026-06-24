package org.example.domain.characterextraction.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.example.domain.character.dto.CharacterResponseDto;
import org.example.domain.character.entity.Character;
import org.example.domain.character.repository.CharacterRepository;
import org.example.domain.characterextraction.dto.CharacterCandidateDto;
import org.example.domain.characterextraction.dto.CharacterExtractionResponseDto;
import org.example.domain.episode.entity.Episode;
import org.example.domain.episode.repository.EpisodeRepository;
import org.example.domain.novel.entity.Novel;
import org.example.domain.user.entity.User;
import org.example.domain.user.repository.UserRepository;
import org.example.global.ai.service.OpenAiService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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
            "5. newInsights는 기존 인물에만 사용하며, 기존 정보에 없는 새로운 성격·말투만 리스트로 포함하세요.\n" +
            "6. evidence는 해당 특성을 뒷받침하는 회차 내 구체적인 장면이나 대사를 기록하세요.\n" +
            "7. age는 반드시 숫자(정수) 또는 null로 설정하세요. 문자열로 작성하지 마세요.";

    private final EpisodeRepository episodeRepository;
    private final CharacterRepository characterRepository;
    private final UserRepository userRepository;
    private final OpenAiService openAiService;
    private final ObjectMapper objectMapper;

    @Transactional(readOnly = true)
    public CharacterExtractionResponseDto extractCharacters(String email, Long episodeId) {
        User user = findUserByEmail(email);
        Episode episode = findEpisodeById(episodeId);
        Novel novel = episode.getNovel();
        validateOwner(novel, user);

        // 기존 등장인물 목록 — AI가 신규/기존 인물을 구분하는 데 사용
        List<Character> existingCharacters = characterRepository.findAllByNovelOrderByNameAsc(novel);

        // OpenAI 호출 — DB 저장 없음
        String input = buildInput(episode, novel, existingCharacters);
        String aiResponse = openAiService.generateText(EXTRACTION_INSTRUCTIONS, input);
        List<CharacterCandidateDto> candidates = parseJson(aiResponse);

        // matchedCharacterId로 기존 Character를 조회해 existingCharacter 필드 보강
        Map<Long, Character> characterMap = existingCharacters.stream()
                .collect(Collectors.toMap(Character::getId, c -> c));

        for (CharacterCandidateDto candidate : candidates) {
            if (Boolean.TRUE.equals(candidate.getIsExistingCharacter())
                    && candidate.getMatchedCharacterId() != null) {
                Character matched = characterMap.get(candidate.getMatchedCharacterId());
                if (matched != null) {
                    candidate.setExistingCharacter(CharacterResponseDto.from(matched));
                }
            }
        }

        String episodeTitle = episode.getEpisodeNumber() + "화 - " + episode.getTitle();
        return new CharacterExtractionResponseDto(episodeTitle, candidates.size(), candidates);
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
                            map.put("personality", c.getPersonality() != null ? c.getPersonality() : "");
                            map.put("speechStyle", c.getSpeechStyle() != null ? c.getSpeechStyle() : "");
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
        sb.append("\"newInsights\":{\"personality\":[\"새 성격\"],\"speechStyle\":[\"새 말투\"]} 또는 null");
        sb.append("}]");

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
            return objectMapper.readValue(cleaned, new TypeReference<List<CharacterCandidateDto>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("AI 응답 파싱에 실패했습니다. 다시 시도해 주세요. 원인: " + e.getMessage());
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
